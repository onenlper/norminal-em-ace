package pronounEM;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import model.Entity;
import model.EntityMention;
import model.EntityMention.Grammatic;
import model.EntityMention.MentionType;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.CoNLL.CoNLLWord;
import model.syntaxTree.MyTreeNode;
import util.Common;
import util.Util;
import ace.ACECommon;
import ace.PlainText;
import ace.reader.ACEReader;

public class ApplyPronounEM {

	String folder;

	Parameter numberP;
	Parameter genderP;
	Parameter animacyP;
	Parameter personP;
	Parameter personQP;

	double contextOverall;

	HashMap<String, Double> contextPrior;
	int overallGuessPronoun;
	HashMap<Short, Double> pronounPrior;
	HashMap<Integer, HashMap<Short, Integer>> counts;
	HashMap<Integer, Integer> denomCounts;
	HashMap<Integer, HashSet<Integer>> subSpace;
	HashMap<String, Double> fracContextCount;

	@SuppressWarnings("unchecked")
	public ApplyPronounEM(String folder) {
		this.folder = folder;
		try {
			ObjectInputStream modelInput = new ObjectInputStream(
					new FileInputStream("EMModelPronoun"));
			numberP = (Parameter) modelInput.readObject();
			genderP = (Parameter) modelInput.readObject();
			animacyP = (Parameter) modelInput.readObject();
			personP = (Parameter) modelInput.readObject();
			personQP = (Parameter) modelInput.readObject();
			fracContextCount = (HashMap<String, Double>) modelInput
					.readObject();
			contextPrior = (HashMap<String, Double>) modelInput.readObject();

			Context.ss = (HashSet<String>) modelInput.readObject();
			Context.vs = (HashSet<String>) modelInput.readObject();
			modelInput.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public ArrayList<ArrayList<EntityMention>> test() {
		ArrayList<String> files = Common.getLines("ACE_Chinese_test"
				+ Util.part);
		ArrayList<ArrayList<Entity>> goldEntities = new ArrayList<ArrayList<Entity>>();
		ACEReader.nerElementses = null;
		ACEReader.testFiles = null;
		
		ArrayList<ArrayList<EntityMention>> anaphorses = new ArrayList<ArrayList<EntityMention>>(); 
		
		for (int g = 0; g < files.size(); g++) {
			String file = files.get(g);
//			System.out.println(file);
			
			CoNLLDocument document = ACEReader.read(file, false);
 			for (int k = 0; k < document.getParts().size(); k++) {
				CoNLLPart part = document.getParts().get(k);

				ArrayList<Entity> goldChains = part.getChains();
				goldEntities.add(goldChains);

				HashMap<String, Integer> chainMap = EMUtil
						.formChainMap(goldChains);

				ArrayList<EntityMention> mentions = Util.getSieveCorefMentions(
						part, files, g);
				Collections.sort(mentions);

				ArrayList<EntityMention> anaphors = new ArrayList<EntityMention>();
				for (EntityMention m : mentions) {
					em.EMUtil.setMentionAttri(m, part);
					if (EMUtil.pronouns.contains(m.extent)) {
						anaphors.add(m);
					}
				}

				findAntecedent(part, chainMap, anaphors, mentions);
				
				anaphorses.add(anaphors);
			}
		}
		return anaphorses;
	}

	private void findAntecedent(CoNLLPart part,
			HashMap<String, Integer> chainMap,
			ArrayList<EntityMention> anaphors,
			ArrayList<EntityMention> allCandidates) {
		for (EntityMention anaphor : anaphors) {
			anaphor.sentenceID = part.getWord(anaphor.start).sentence
					.getSentenceIdx();
			anaphor.s = part.getWord(anaphor.start).sentence;

			EntityMention antecedent = null;
			double maxP = -1;
			Collections.sort(allCandidates);
			String proSpeaker = part.getWord(anaphor.start).speaker;
			String overtPro = "";

			ArrayList<EntityMention> cands = new ArrayList<EntityMention>();
			boolean findFS = false;

			String taMSg = "";
			String bestMSg = "";
			for (int h = allCandidates.size() - 1; h >= 0; h--) {
				EntityMention cand = allCandidates.get(h);
				String antSpeaker = part.getWord(cand.start).speaker;
				cand.sentenceID = part.getWord(cand.start).sentence
						.getSentenceIdx();
				cand.s = part.getWord(cand.start).sentence;
				cand.isFS = false;
				cand.isBest = false;
				cand.MI = Context.calMI(cand, anaphor);
				if (cand.start < anaphor.start
						&& anaphor.sentenceID - cand.sentenceID <= 2) {
					if (!findFS && cand.gram == Grammatic.subject
					// && !cand.s.getWord(cand.headInS).posTag.equals("NT")
					// && MI>0
					) {
						cand.isFS = true;
						findFS = true;
					}

					cands.add(cand);
				}
			}

			boolean findBest = findBest(anaphor, cands);

			int chose = -1;
			double votes[] = new double[cands.size()];
			String pronoun = anaphor.head;
			anaphor.extent = pronoun;
			double norm = 0;

			double probs[] = new double[cands.size()];

			for (int i = 0; i < cands.size(); i++) {
				EntityMention cand = cands.get(i);
				if (cand.extent.isEmpty()) {
					continue;
				}

				String antSpeaker = part.getWord(cand.start).speaker;
				cand.sentenceID = part.getWord(cand.start).sentence
						.getSentenceIdx();
				boolean coref = chainMap.containsKey(anaphor.toName())
						&& chainMap.containsKey(cand.toName())
						&& chainMap.get(anaphor.toName()).intValue() == chainMap
								.get(cand.toName()).intValue();

				// calculate P(overt-pronoun|ant-context)
				String ant = cand.head;

				// TODO
				Context context = Context.buildContext(cand, anaphor, part,
						cand.isFS);
				cand.msg = Context.message;
				cand.MI = Context.MI;

				boolean sameSpeaker = proSpeaker.equals(antSpeaker);
				double p_person = 0;
				if (sameSpeaker) {
					p_person = personP.getVal(EMUtil.getAntPerson(ant).name(),
							EMUtil.getPerson(pronoun).name());
				} else {
					p_person = personQP.getVal(EMUtil.getAntPerson(ant).name(),
							EMUtil.getPerson(pronoun).name());
				}
				cand.person = EMUtil.getAntPerson(ant);
				double p_number = numberP.getVal(EMUtil.getAntNumber(cand)
						.name(), EMUtil.getNumber(pronoun).name());
				cand.number = EMUtil.getAntNumber(cand);
				double p_animacy = animacyP.getVal(EMUtil.getAntAnimacy(cand)
						.name(), EMUtil.getAnimacy(pronoun).name());
				cand.animacy = EMUtil.getAntAnimacy(cand);
				double p_gender = genderP.getVal(EMUtil.getAntGender(cand)
						.name(), EMUtil.getGender(pronoun).name());
				cand.gender = EMUtil.getAntGender(cand);

				double p_context = 0.0000000000000000000000000000000000000000000001;
				if (fracContextCount.containsKey(context.toString())) {
					p_context = (1.0 * EMUtil.alpha + fracContextCount
							.get(context.toString()))
							/ (2.0 * EMUtil.alpha + contextPrior.get(context
									.toString()));
				} else {
					p_context = 1.0 / 2.0;
				}

				double p2nd = p_person * p_number * p_gender * p_animacy
						* p_context * 1;

				if (pronoun.equals("它")) {
					String msg = p_person + "\t" + p_number + "\t" + p_gender
							+ "\t" + p_animacy + "\t" + p_context;
				}

				double p = p2nd;
				norm += p;
				probs[i] = p;

				if (p > maxP) {
					antecedent = cand;
					maxP = p;
					overtPro = pronoun;
					bestMSg = p_person + "\t" + p_number + "\t" + p_gender
							+ "\t" + p_animacy + "\t" + p_context;
					chose = i;
				}
			}

			if (antecedent != null) {
				if (antecedent.end != -1) {
					anaphor.antecedent = antecedent;
				} else {
					anaphor.antecedent = antecedent.antecedent;
				}
				anaphor.extent = antecedent.extent;
				anaphor.head = antecedent.head;
				anaphor.gram = Grammatic.subject;
				anaphor.mType = antecedent.mType;
				anaphor.NE = antecedent.NE;
				// System.out.println(zero.start);
				// System.out.println(antecedent.extent);
			}
			if (anaphor.antecedent != null
					&& anaphor.antecedent.end != -1
					&& chainMap.containsKey(anaphor.toName())
					&& chainMap.containsKey(anaphor.antecedent.toName())
					&& chainMap.get(anaphor.toName()).intValue() == chainMap
							.get(anaphor.antecedent.toName()).intValue()) {
				String key = part.docName + ":" + part.getPartID() + ":"
						+ anaphor.start + "-" + anaphor.antecedent.start + ","
						+ anaphor.antecedent.end + ":GOOD";
				corrects.add(key);
				// if(antecedent.mType==EntityMentionType.tmporal) {
				// System.out.println(antecedent.extent + "GOOD!");
				// }
				// System.out.println(overtPro + "  " + zero.antecedent.extent);
				// System.out.println("+++");
				// printResult(zero, zero.antecedent, part);
				// System.out.println("Predicate: " +
				// this.getPredicate(zero.V));
				// System.out.println("Object NP: " +
				// this.getObjectNP(zero));
				// System.out.println("===");
				// if (zero.antecedent.MI < 0) {
				// System.out.println("Right!!! " + good + "/" + bad);
				// System.out.println(zero.antecedent.msg);
				// if(!zero.antecedent.isFS) {
//				System.out.println("==========");
//				if (anaphor.antecedent != null) {
//					System.out.println(anaphor.antecedent.extent + ":"
//							+ anaphor.antecedent.NE + "#"
//							+ anaphor.antecedent.number + "#"
//							+ anaphor.antecedent.gender + "#"
//							+ anaphor.antecedent.person + "#"
//							+ anaphor.antecedent.animacy);
//					System.out.println(anaphor);
//					printResult(anaphor, anaphor.antecedent, part);
//					System.out.println(overtPro + "#");
//				}
				// System.out.println(overtPro + "#" + bestMSg);
				// System.out.println("它: " + taMSg);
				// }
				// }
			} else {
				if (anaphor.antecedent == null) {
					String key = part.docName + ":" + part.getPartID() + ":"
							+ anaphor.start + "-NULL:BAD";
					corrects.add(key);
				} else {
					String key = part.docName + ":" + part.getPartID() + ":"
							+ anaphor.start + "-" + anaphor.antecedent.start
							+ "," + anaphor.antecedent.end + ":BAD";
					corrects.add(key);
				}
				// if(antecedent!=null &&
				// antecedent.mType==EntityMentionType.tmporal)
				// {
				// System.out.println(antecedent.extent + "BAD !");
				// }
//				System.out.println("==========");
//				if (anaphor.antecedent != null) {
//					System.out.println(anaphor.antecedent.extent + ":"
//							+ anaphor.antecedent.NE + "#"
//							+ anaphor.antecedent.number + "#"
//							+ anaphor.antecedent.gender + "#"
//							+ anaphor.antecedent.person + "#"
//							+ anaphor.antecedent.animacy);
//					System.out.println(anaphor);
//					printResult(anaphor, anaphor.antecedent, part);
//					System.out.println(overtPro + "#" + bestMSg);
//					System.out.println("它: " + taMSg);
//				}
			}

		}

	}

	protected void printResult(EntityMention zero, EntityMention systemAnte,
			CoNLLPart part) {
		StringBuilder sb = new StringBuilder();
		CoNLLSentence s = part.getWord(zero.start).sentence;
		CoNLLWord word = part.getWord(zero.start);
		for (int i = word.indexInSentence; i < s.words.size(); i++) {
			sb.append(s.words.get(i).word).append(" ");
		}
		System.out.println(sb.toString() + " # " + zero.start);
		System.out.println(systemAnte != null ? systemAnte.extent + "#"
				+ part.getWord(systemAnte.end + 1).word : "");

		// System.out.println("========");
	}

	// public double getMaxEntProb(EntityMention cand, EntityMention pro,
	// boolean sameSpeaker,
	// Context context, CoNLLPart part) {
	// String pronoun = pro.extent;
	// String pStr = "";
	// if (sameSpeaker) {
	// pStr = EMUtil.getAntPerson(cand.head).name() + "="
	// + EMUtil.getPerson(pronoun).name();
	// } else {
	// pStr = EMUtil.getAntPerson(cand.head).name() + "!="
	// + EMUtil.getPerson(pronoun).name();
	// }
	// String nStr = EMUtil.getAntNumber(cand).name() + "="
	// + EMUtil.getNumber(pronoun).name();
	// String aStr = EMUtil.getAntAnimacy(cand).name() + "="
	// + EMUtil.getAnimacy(pronoun).name();
	// String gStr = EMUtil.getAntGender(cand).name() + "="
	// + EMUtil.getGender(pronoun).name();
	// superFea.configure(pStr, nStr, gStr, aStr, context, cand, pro, part);
	//
	// String svm = superFea.getSVMFormatString();
	// svm = "-1 " + svm;
	// Datum<String, String> testIns = Dataset.svmLightLineToDatum(svm);
	// // Datum<String, String> testIns =
	// // EMUtil.svmlightToStanford(superFea.getFeas(), "-1");
	// Counter<String> scores = classifier.scoresOf(testIns);
	// Distribution<String> distr = Distribution
	// .distributionFromLogisticCounter(scores);
	// double prob = distr.getCount("+1");
	// return prob;
	// }

	public void addEmptyCategoryNode(EntityMention zero) {
		MyTreeNode V = zero.V;
		MyTreeNode newNP = new MyTreeNode();
		newNP.value = "NP";
		int VIdx = V.childIndex;
		V.parent.addChild(VIdx, newNP);

		MyTreeNode empty = new MyTreeNode();
		empty.value = "-NONE-";
		newNP.addChild(empty);

		MyTreeNode child = new MyTreeNode();
		child.value = zero.extent;
		empty.addChild(child);
		child.emptyCategory = true;
		zero.NP = newNP;
	}

	public static boolean findBest(EntityMention zero,
			ArrayList<EntityMention> cands) {
		boolean findBest = false;
		for (int i = 0; i < cands.size(); i++) {
			EntityMention cand = cands.get(i);
			if (cand.gram == Grammatic.subject && cand.MI > 0
					&& cand.s == zero.s) {
				findBest = true;
				cand.isBest = true;
				break;
			}
		}

		if (!findBest) {
			for (int i = 0; i < cands.size(); i++) {
				EntityMention cand = cands.get(i);
				if (cand.MI > 0 && cand.s == zero.s) {
					findBest = true;
					cand.isBest = true;
					break;
				}
			}
		}

		if (!findBest) {
			for (int i = 0; i < cands.size(); i++) {
				EntityMention cand = cands.get(i);
				if (cand.MI > 0 && cand.gram == Grammatic.subject) {
					findBest = true;
					cand.isBest = true;
					break;
				}
			}
		}

		if (!findBest) {
			for (int i = 0; i < cands.size(); i++) {
				EntityMention cand = cands.get(i);
				if (cand.MI > 0) {
					findBest = true;
					cand.isBest = true;
					break;
				}
			}
		}
		return findBest;
	}

	static ArrayList<String> corrects = new ArrayList<String>();

	public static void main(String args[]) {
		if (args.length != 1) {
			System.err.println("java ~ folder");
			System.exit(1);
		}
		Util.part = args[0];
		run(args[0]);
	}

	public static ArrayList<ArrayList<EntityMention>> run(String folder) {
		EMUtil.train = false;
		ApplyPronounEM test = new ApplyPronounEM(folder);
		ArrayList<ArrayList<EntityMention>> anaphorses = test.test();
		return anaphorses;
	}
}
