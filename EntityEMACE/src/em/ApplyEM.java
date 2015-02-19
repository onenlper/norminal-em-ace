package em;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import pronounEM.ApplyPronounEM;

import model.Entity;
import model.EntityMention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.CoNLL.CoNLLWord;
import util.Common;
import util.Util;
import ace.ACECommon;
import ace.PlainText;
import ace.reader.ACEReader;
import edu.stanford.nlp.classify.LinearClassifier;
import em.ResolveGroup.Entry;

public class ApplyEM {

	String folder;

	Parameter numberP;
	Parameter genderP;
	Parameter animacyP;
	Parameter semanticP;
	Parameter gramP;
	Parameter cilinP;

	double contextOverall;

	HashMap<String, Double> contextPrior;

	int overallGuessPronoun;

	HashMap<Short, Double> pronounPrior;
	HashMap<Integer, HashMap<Short, Integer>> counts;
	HashMap<Integer, Integer> denomCounts;
	HashMap<Integer, HashSet<Integer>> subSpace;

	HashMap<String, Double> fracContextCount;

	LinearClassifier<String, String> classifier;

	@SuppressWarnings("unchecked")
	public ApplyEM(String folder) {
		this.folder = folder;
		try {
			ObjectInputStream modelInput = new ObjectInputStream(
					new FileInputStream("EMModel"));
			numberP = (Parameter) modelInput.readObject();
			genderP = (Parameter) modelInput.readObject();
			animacyP = (Parameter) modelInput.readObject();
			semanticP = (Parameter) modelInput.readObject();
			gramP = (Parameter) modelInput.readObject();
			cilinP = (Parameter) modelInput.readObject();
			fracContextCount = (HashMap<String, Double>) modelInput
					.readObject();
			contextPrior = (HashMap<String, Double>) modelInput.readObject();

			Context.ss = (HashSet<String>) modelInput.readObject();
			Context.vs = (HashSet<String>) modelInput.readObject();
			// Context.svoStat = (SVOStat)modelInput.readObject();
			modelInput.close();

			// ObjectInputStream modelInput2 = new ObjectInputStream(
			// new FileInputStream("giga2/EMModel"));
			// numberP = (Parameter) modelInput2.readObject();
			// genderP = (Parameter) modelInput2.readObject();
			// animacyP = (Parameter) modelInput2.readObject();
			// personP = (Parameter) modelInput2.readObject();
			// personQP = (Parameter) modelInput2.readObject();
			// fracContextCount = (HashMap<String, Double>) modelInput2
			// .readObject();
			// contextPrior = (HashMap<String, Double>)
			// modelInput2.readObject();

			// modelInput2.close();
			// loadGuessProb();
			// EMUtil.loadPredictNE(folder, "test");
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

	public static ArrayList<String> goods = new ArrayList<String>();
	public static ArrayList<String> bads = new ArrayList<String>();

	double good = 0;
	double bad = 0;

	public void test() {
		ArrayList<String> files = Common.getLines("ACE_Chinese_test"
				+ Util.part);

		ArrayList<String> fileNames = new ArrayList<String>();
		ArrayList<Integer> lengths = new ArrayList<Integer>();
		ArrayList<ArrayList<Entity>> answers = new ArrayList<ArrayList<Entity>>();
		ArrayList<ArrayList<Entity>> goldKeys = new ArrayList<ArrayList<Entity>>();

		ArrayList<ArrayList<EntityMention>> pronounses = ApplyPronounEM.run(Util.part);
		
		ACEReader.nerElementses = null;
		ACEReader.testFiles = null;
		for (int g = 0; g < files.size(); g++) {
			if (g % 5 != part) {
				// continue;
			}
			String file = files.get(g);
			ArrayList<EntityMention> pronouns = pronounses.get(g);

			CoNLLDocument document = ACEReader.read(file, false);
			PlainText plainText = ACECommon.getPlainText(file + ".sgm");

			fileNames.add(file.replace(
					"/users/yzcchen/chen3/coling2012/LDC2006T06/data/Chinese/",
					"/users/yzcchen/ACL12/data/ACE2005/Chinese/")
					+ ".sgm");
			lengths.add(plainText.content.length());

			document.language = "chinese";

			for (int k = 0; k < document.getParts().size(); k++) {
				CoNLLPart part = document.getParts().get(k);

				ArrayList<Entity> goldChains = part.getChains();

				HashMap<String, Integer> chainMap = EMUtil
						.formChainMap(goldChains);

				ArrayList<EntityMention> goldBoundaryNPMentions = Util
						.getSieveCorefMentions(part, files, g);

				for (EntityMention m : goldBoundaryNPMentions) {
					EMUtil.setMentionAttri(m, part);
				}

				Collections.sort(goldBoundaryNPMentions);

				ArrayList<EntityMention> candidates = new ArrayList<EntityMention>();
				ArrayList<EntityMention> anaphors = new ArrayList<EntityMention>();
				for (EntityMention m : goldBoundaryNPMentions) {
					if (!pronounEM.EMUtil.pronouns.contains(m.extent)) {
						candidates.add(m);
						anaphors.add(m);
					}
				}

				Collections.sort(candidates);

				findAntecedent(file, part, chainMap, anaphors, candidates);

				ArrayList<Entity> activeChains = new ArrayList<Entity>();
				for (EntityMention m : anaphors) {
					if (m.antecedent == null || m.antecedent.isFake) {
						Entity entity = new Entity();
						entity.addMention(m);
						activeChains.add(entity);
					} else {
						out: for (Entity chain : activeChains) {
							for (EntityMention ant : chain.mentions) {
								if (m.antecedent == ant) {
									chain.mentions.add(m);
									break out;
								}
							}
						}
					}
				}
				
				Collections.sort(pronouns);
				for(EntityMention pro : pronouns) {
					if(pro.antecedent==null) {
						Entity entity = new Entity();
						entity.addMention(pro);
						activeChains.add(entity);
					} else {
						out: for (Entity chain : activeChains) {
							for (EntityMention ant : chain.mentions) {
								if (pro.antecedent.equals(ant)) {
									chain.mentions.add(pro);
									break out;
								}
							}
						}
					}
				}
				
				answers.add(activeChains);
				goldKeys.add(part.getChains());
			}
		}
		System.out.println("Good: " + good);
		System.out.println("Bad: " + bad);
		System.out.println("Precission: " + good / (good + bad) * 100);
		try {
			ToSemEval.outputSemFormatEntity(fileNames, lengths, "entity.sys."
					+ Util.part, answers);
			ToSemEval.outputSemFormatEntity(fileNames, lengths, "entity.gold."
					+ Util.part, goldKeys);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// evaluate(corefResults, goldKeyses);
		// int all = 0;
		// for (String key : maps.keySet()) {
		// all += maps.get(key).size();
		// }
		// System.out.println(all + "@@@");
		// System.out.println(all2 + "@@@");
		//
		// System.out.println(ApplyEM.allL);
		// System.out.println(zeroAnt + "/" + allAnt + ":" + zeroAnt / allAnt);
		// System.out.println("Bad_P_C:" + badP_C);
	}

	static double min_amongMax = 1;

	static ArrayList<String> goodAnas = new ArrayList<String>();

	static double allAnt = 0;
	static double zeroAnt = 0;

	static double badP_C = 0;

	static HashMap<String, HashSet<String>> chainMaps = new HashMap<String, HashSet<String>>();

	private void findAntecedent(String file, CoNLLPart part,
			HashMap<String, Integer> chainMap,
			ArrayList<EntityMention> anaphors,
			ArrayList<EntityMention> allCandidates) {
		for (EntityMention anaphor : anaphors) {
			anaphor.sentenceID = part.getWord(anaphor.start).sentence
					.getSentenceIdx();
			anaphor.s = part.getWord(anaphor.start).sentence;

			EntityMention antecedent = null;
			double maxP = 0;
			Collections.sort(allCandidates);

			ArrayList<EntityMention> cands = new ArrayList<EntityMention>();

			for (int h = allCandidates.size() - 1; h >= 0; h--) {
				EntityMention cand = allCandidates.get(h);
				cand.sentenceID = part.getWord(cand.start).sentence
						.getSentenceIdx();
				cand.s = part.getWord(cand.start).sentence;

				if (cand.start < anaphor.start
						&& anaphor.sentenceID - cand.sentenceID <= EMLearn.maxDistance
						&& cand.end != anaphor.end
				// && !predictBadOnes.contains(part.getPartName() + ":" +
				// cand.toName())
				) {

					cands.add(cand);
				}
			}
			EntityMention fake = new EntityMention();
			fake.extent = "fakkkkke";
			fake.head = "fakkkkke";
			fake.isFake = true;
			cands.add(fake);

			ResolveGroup rg = new ResolveGroup(anaphor, part, cands);
			int seq = 0;
			for (EntityMention cand : cands) {
				Entry entry = new Entry(cand, null, part);
				rg.entries.add(entry);
				entry.p_c = EMUtil.getP_C(cand, anaphor, part);
				if (entry.p_c != 0) {
					seq += 1;
				}

				if (!chainMaps.containsKey(entry.antName) && !entry.isFake) {
					HashSet<String> set = new HashSet<String>();
					set.add(entry.antName);
					chainMaps.put(entry.antName, set);
				}
			}
			for (Entry entry : rg.entries) {
				if (entry.isFake) {
					entry.p_c = Entry.p_fake_decay / (Entry.p_fake_decay + seq);
				} else if (entry.p_c != 0) {
					entry.p_c = 1 / (Entry.p_fake_decay + seq);
				}
			}

			EMLearn.sortEntries(rg, chainMaps);

			double probs[] = new double[cands.size()];

			ArrayList<EntityMention> goldCorefs = new ArrayList<EntityMention>();

			for (int i = 0; i < rg.entries.size(); i++) {
				Entry entry = rg.entries.get(i);
				EntityMention cand = cands.get(i);

				boolean coref = chainMap.containsKey(anaphor.toName())
						&& chainMap.containsKey(cand.toName())
						&& chainMap.get(anaphor.toName()).intValue() == chainMap
								.get(cand.toName()).intValue();
				Context.coref = coref;
				Context.gM1 = chainMap.containsKey(cand.toName());
				Context.gM2 = chainMap.containsKey(anaphor.toName());
				entry.context = Context.buildContext(cand, anaphor, part,
						cands, entry.seq);
				cand.msg = Context.message;

				allAnt++;
				if (entry.p_c == 0) {
					if (coref) {
						badP_C++;
					}
					zeroAnt++;
				}
				if (coref && entry.p_c != 0) {
					goldCorefs.add(cand);
				}
			}

			// TODO
			String antName = "";
			for (int i = 0; i < rg.entries.size(); i++) {
				Entry entry = rg.entries.get(i);
				EntityMention cand = entry.ant;
				Context context = entry.context;

				double p_sem = semanticP.getVal(entry.sem,
						EMUtil.getSemantic(anaphor));

				double p_context = 0.0000000000000000000000000000000000000000000001;
				if (fracContextCount.containsKey(context.toString())) {
					p_context = (1.0 * EMUtil.alpha + fracContextCount
							.get(context.toString()))
							/ (2.0 * EMUtil.alpha + contextPrior.get(context
									.toString()));
				} else {
					p_context = 1.0 / 2;
				}

				double p2nd = p_context * entry.p_c;
				p2nd *= 1 * p_sem;
				double p = p2nd;
				probs[i] = p;
				
				if (p > maxP && p != 0) {
					antecedent = cand;
					maxP = p;
					antName = entry.antName;
				}
			}

			if (antecedent != null && !antecedent.isFake
					&& anaphor.antecedent == null) {
				HashSet<String> corefs = chainMaps.get(antName);
				corefs.add(rg.anaphorName);
				chainMaps.put(rg.anaphorName, corefs);

				anaphor.antecedent = antecedent;

				boolean coref = chainMap.containsKey(anaphor.toName())
						&& chainMap.containsKey(antecedent.toName())
						&& chainMap.get(anaphor.toName()).intValue() == chainMap
								.get(antecedent.toName()).intValue();

				if (!coref && goldCorefs.size() != 0) {
					// anaphor.antecedent= goldCorefs.get(0);
//					System.out.println("Anaphor: " + anaphor.head + " "
//							+ anaphor.semClass + " # " + anaphor.subType
//							+ " # " + chainMap.containsKey(anaphor.toName()));
//					System.out.println("Selected: " + antecedent.head + " "
//							+ antecedent.semClass + " # "
//							+ antecedent.subType + " # "
//							+ chainMap.containsKey(antecedent.toName()));
//					System.out.println("True Ante: ");
//					for (EntityMention m : goldCorefs) {
//						System.out.println(m.head + " " + m.semClass + " # "
//								+ m.subType);
//					}
//					System.out.println("---------------------------");
					// print(antecedent, anaphor, part, chainMap);
				}

				if (!coref && goldCorefs.size() == 0) {
					// anaphor.antecedent= null;
//					System.out.println("Anaphor: " + anaphor.head + " "
//							+ anaphor.semClass + " # " + anaphor.subType
//							+ " # " + chainMap.containsKey(anaphor.toName()));
//					System.out.println("Selected: " + antecedent.head + " "
//							+ antecedent.semClass + " # "
//							+ antecedent.subType + " # "
//							+ chainMap.containsKey(antecedent.toName()));
//					System.out.println("True Ante: EMPTY");
//					System.out.println("---------------------------");
					// print(antecedent, anaphor, part, chainMap);
				}

			}
		}
	}

	public static void print(EntityMention antecedent, EntityMention anaphor,
			CoNLLPart part, HashMap<String, Integer> chainMap) {
		System.out.println(antecedent.extent + " # "
				+ chainMap.containsKey(antecedent.toName()));
		System.out.println(antecedent.s.getText());
		System.out.println(anaphor.extent + " # "
				+ chainMap.containsKey(anaphor.toName()));
		System.out.println(anaphor.s.getText());
		System.out.println(part.getDocument().getFilePath()
				.replace("v5_auto_conll", "v4_gold_conll"));
		System.out.println("----");
	}

	static int allL = 0;

	protected void printResult(EntityMention zero, EntityMention systemAnte,
			CoNLLPart part) {
		StringBuilder sb = new StringBuilder();
		CoNLLSentence s = part.getWord(zero.start).sentence;
		CoNLLWord word = part.getWord(zero.start);
		for (int i = word.indexInSentence; i < s.words.size(); i++) {
			sb.append(s.words.get(i).word).append(" ");
		}
		System.out.println(sb.toString() + " # " + zero.start);
		// System.out.println("========");
	}

	// public void addEmptyCategoryNode(Mention zero) {
	// MyTreeNode V = zero.V;
	// MyTreeNode newNP = new MyTreeNode();
	// newNP.value = "NP";
	// int VIdx = V.childIndex;
	// V.parent.addChild(VIdx, newNP);
	//
	// MyTreeNode empty = new MyTreeNode();
	// empty.value = "-NONE-";
	// newNP.addChild(empty);
	//
	// MyTreeNode child = new MyTreeNode();
	// child.value = zero.extent;
	// empty.addChild(child);
	// child.emptyCategory = true;
	// zero.NP = newNP;
	// }

	static String prefix = "/shared/mlrdir1/disk1/mlr/corpora/CoNLL-2012/conll-2012-train-v0/data/files/data/chinese/annotations/";
	static String anno = "annotations/";
	static String suffix = ".coref";

	// private static ArrayList<Mention> getGoldNouns(ArrayList<Entity>
	// entities,
	// CoNLLPart goldPart) {
	// ArrayList<Mention> goldAnaphors = new ArrayList<Mention>();
	// for (Entity e : entities) {
	// Collections.sort(e.mentions);
	// for (int i = 1; i < e.mentions.size(); i++) {
	// Mention m1 = e.mentions.get(i);
	// String pos1 = goldPart.getWord(m1.end).posTag;
	// if (pos1.equals("PN") || pos1.equals("NR") || pos1.equals("NT")) {
	// continue;
	// }
	// goldAnaphors.add(m1);
	// }
	// }
	// Collections.sort(goldAnaphors);
	// for (Mention m : goldAnaphors) {
	// EMUtil.setMentionAttri(m, goldPart);
	// }
	// return goldAnaphors;
	// }
	//
	// private static ArrayList<Mention> getGoldAnaphorNouns(
	// ArrayList<Entity> entities, CoNLLPart goldPart) {
	// ArrayList<Mention> goldAnaphors = new ArrayList<Mention>();
	// for (Entity e : entities) {
	// Collections.sort(e.mentions);
	// for (int i = 1; i < e.mentions.size(); i++) {
	// Mention m1 = e.mentions.get(i);
	// String pos1 = goldPart.getWord(m1.end).posTag;
	// if (pos1.equals("PN") || pos1.equals("NR") || pos1.equals("NT")) {
	// continue;
	// }
	// HashSet<String> ants = new HashSet<String>();
	// for (int j = i - 1; j >= 0; j--) {
	// Mention m2 = e.mentions.get(j);
	// String pos2 = goldPart.getWord(m2.end).posTag;
	// if (!pos2.equals("PN") && m1.end != m2.end) {
	// ants.add(m2.toName());
	// }
	// }
	// if (ants.size() != 0) {
	// goldAnaphors.add(m1);
	// }
	// }
	// }
	// Collections.sort(goldAnaphors);
	// for (Mention m : goldAnaphors) {
	// EMUtil.setMentionAttri(m, goldPart);
	// }
	// return goldAnaphors;
	// }

	public static void evaluate(
			HashMap<String, ArrayList<EntityMention>> anaphorses,
			HashMap<String, HashMap<String, HashSet<String>>> goldKeyses) {
		double gold = 0;
		double system = 0;
		double hit = 0;

		for (String key : anaphorses.keySet()) {
			ArrayList<EntityMention> anaphors = anaphorses.get(key);
			HashMap<String, HashSet<String>> keys = goldKeyses.get(key);
			gold += keys.size();
			system += anaphors.size();
			for (EntityMention anaphor : anaphors) {
				EntityMention ant = anaphor.antecedent;
				if (keys.containsKey(anaphor.toName())
						&& keys.get(anaphor.toName()).contains(ant.toName())) {
					hit++;
				}
			}
		}

		double r = hit / gold;
		double p = hit / system;
		double f = 2 * r * p / (r + p);
		System.out.println("============");
		System.out.println("Hit: " + hit);
		System.out.println("Gold: " + gold);
		System.out.println("System: " + system);
		System.out.println("============");
		System.out.println("Recall: " + r * 100);
		System.out.println("Precision: " + p * 100);
		System.out.println("F-score: " + f * 100);
	}

	static ArrayList<String> corrects = new ArrayList<String>();
	static HashSet<String> predictBadOnes;

	public static void main(String args[]) {
		if (args.length != 1) {
			System.err.println("java ~ folder");
			System.exit(1);
		}
		// EMUtil.loadAlign();
		Util.part = args[0];
		ArrayList<String> allMs = Common.getLines("allMs");
		ArrayList<String> preds = Common
				.getLines("/users/yzcchen/tool/svmlight/svm.anaphor.pred");
		predictBadOnes = new HashSet<String>();
		for (int i = 0; i < allMs.size(); i++) {
			String m = allMs.get(i);
			String pred = preds.get(i);
			if (Double.parseDouble(pred) < 0) {
				predictBadOnes.add(m);
			}
		}
		run(args[0]);
		// run("nw");
		// run("mz");
		// run("wb");
		// run("bn");
		// run("bc");
		// run("tc");
	}

	static int part;

	public static void run(String folder) {
		EMUtil.train = false;
		ApplyEM test = new ApplyEM(folder);

		// for(int i=0;i<5;i++) {
		// part = i;
		test.test();
		// Common.pause("!!#");
		// }

		System.out.println("RUNN: " + folder);
		Common.outputHashSet(Context.todo, "todo.word2vec");
		if (Context.todo.size() != 0) {
			System.out.println("!!!!! TODO WORD2VEC!!!!");
			System.out.println("check file: todo.word2vec "
					+ Context.todo.size());
		}
		// Common.outputLines(goodAnas, "goodAnaphors");

		// Common.outputHashSet(EMUtil.semanticInstances, "semanticInstance");
		// Common.pause("!!#");
	}
}
