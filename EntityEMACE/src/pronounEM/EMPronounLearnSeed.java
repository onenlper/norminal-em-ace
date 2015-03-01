package pronounEM;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import model.Entity;
import model.EntityMention;
import model.EntityMention.Animacy;
import model.EntityMention.Gender;
import model.EntityMention.Grammatic;
import model.EntityMention.Number;
import model.EntityMention.Person;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import pronounEM.ResolveGroup.Entry;
import util.Common;
import util.Util;
import ace.ACECorefCommon;
import ace.reader.ACEReader;

public class EMPronounLearnSeed {

	static Parameter numberP;
	static Parameter genderP;
	static Parameter personP;
	static Parameter personQP;
	static Parameter animacyP;

	static HashMap<String, Double> contextPrior;
	static HashMap<String, Double> contextOverall;
	static HashMap<String, Double> fracContextCount;

	static int count = 0;
	
	static ArrayList<HashMap<String, Double>> multiFracContextsCountl0 = new ArrayList<HashMap<String, Double>>();
	static ArrayList<HashMap<String, Double>> multiFracContextsCountl1 = new ArrayList<HashMap<String, Double>>();

	static ArrayList<HashMap<String, Double>> multiFracContextsProbl0 = new ArrayList<HashMap<String, Double>>();
	static ArrayList<HashMap<String, Double>> multiFracContextsProbl1 = new ArrayList<HashMap<String, Double>>();

	public static void init() {
		numberP = new Parameter(1.0 / ((double) Number.values().length));
		genderP = new Parameter(1.0 / ((double) Gender.values().length));
		personP = new Parameter(1.0 / ((double) Person.values().length));
		personQP = new Parameter(1.0 / ((double) Person.values().length));
		animacyP = new Parameter(1.0 / ((double) Animacy.values().length));

		for (int i = 0; i < Context.getSubContext().size(); i++) {
			multiFracContextsCountl0.add(new HashMap<String, Double>());
			multiFracContextsCountl1.add(new HashMap<String, Double>());
			multiFracContextsProbl0.add(new HashMap<String, Double>());
			multiFracContextsProbl1.add(new HashMap<String, Double>());
		}
		
		contextPrior = new HashMap<String, Double>();
		contextOverall = new HashMap<String, Double>();
		fracContextCount = new HashMap<String, Double>();
		count = 0;
		Context.contextCache.clear();
	}

//	public static ArrayList<EntityMention> extractMention(CoNLLPart part,
//			CoNLLSentence s) {
//		ArrayList<EntityMention> ems = new ArrayList<EntityMention>();
//		for (Entity e : part.getChains()) {
//			for (EntityMention m : e.mentions) {
//				if (s.getStartWordIdx() <= m.headStart
//						&& s.getEndWordIdx() >= m.headStart) {
//					ems.add(m);
//				}
//			}
//		}
//		return ems;
//	}

	public static ArrayList<EntityMention> extractMention(
			ArrayList<EntityMention> allMentions, CoNLLSentence s) {
		ArrayList<EntityMention> ems = new ArrayList<EntityMention>();
		for (EntityMention m : allMentions) {
			if (s.getStartWordIdx() <= m.headStart
					&& s.getEndWordIdx() >= m.headStart) {
				ems.add(m);
			}
		}
		return ems;
	}
	
	public static ArrayList<ResolveGroup> extractGroups(CoNLLPart part,
			String docName, ArrayList<EntityMention> entityMentions) {
		for (Entity e : part.getChains()) {
			for (EntityMention m : e.mentions) {
				em.EMUtil.setMentionAttri(m, part);
			}
		}

		ArrayList<ResolveGroup> groups = new ArrayList<ResolveGroup>();
		for (int i = 0; i < part.getCoNLLSentences().size(); i++) {
			CoNLLSentence s = part.getCoNLLSentences().get(i);

			s.mentions = extractMention(entityMentions, s);
			EMUtil.assignNE(s.mentions, part.getNameEntities());

			ArrayList<EntityMention> precedMs = new ArrayList<EntityMention>();

			if (i >= 2) {
				precedMs.addAll(part.getCoNLLSentences().get(i - 2).mentions);
			}
			if (i >= 1) {
				precedMs.addAll(part.getCoNLLSentences().get(i - 1).mentions);
			}

			for (int j = 0; j < s.mentions.size(); j++) {
				EntityMention m = s.mentions.get(j);

				if (pronounEM.EMUtil.pronouns.contains(m.extent)) {
					EMUtil.setPronounAttri(m, part);

					String proSpeaker = part.getWord(m.start).speaker;

					ArrayList<EntityMention> ants = new ArrayList<EntityMention>();
					ants.addAll(precedMs);
					if (j > 0) {
						ants.addAll(s.mentions.subList(0, j - 1));
					}
					ResolveGroup rg = new ResolveGroup(m.extent, part, m.toCharName());
					Collections.sort(ants);
					Collections.reverse(ants);
					boolean findFirstSubj = false;
					// TODO

					for (EntityMention ant : ants) {
						ant.MI = Context.calMI(ant, m);
						ant.isBest = false;
					}

					ApplyPronounEM.findBest(m, ants);

					for (int k = 0; k < ants.size(); k++) {
						EntityMention ant = ants.get(k);
						// add antecedents
						boolean fs = false;
						if (!findFirstSubj && ant.gram == Grammatic.subject
						// && !ant.s.getWord(ant.headInS).posTag.equals("NT")
						) {
							findFirstSubj = true;
							fs = true;
						}

						String antSpeaker = part.getWord(ant.start).speaker;

						Context context = Context
								.buildContext(ant, m, part, fs);

						boolean sameSpeaker = proSpeaker.equals(antSpeaker);
						Entry entry = new Entry(ant, context, sameSpeaker, fs);
						rg.entries.add(entry);
						count++;
						Double d = contextPrior.get(context.toString());
						if (d == null) {
							contextPrior.put(context.toString(), 1.0);
						} else {
							contextPrior.put(context.toString(),
									1.0 + d.doubleValue());
						}
					}
					groups.add(rg);

				}
			}
		}
		return groups;
	}
	
	static double countl0 = 0;
	static double countl1 = 0;

	static double pl0 = 0;
	static double pl1 = 0;

	private static void extractCoNLL(ArrayList<ResolveGroup> groups) {
		ArrayList<String> lines = Common.getLines("/users/yzcchen/chen3/eventBilingual/BilingualEvent/src/ACE_Chinese_train"
				+ Util.part);
		int docNo = 0;

		HashMap<String, ArrayList<EntityMention>> allEntityMentions = em.EMLearnSeed
				.loadSVMResult("6");

		ArrayList<String> activeLines = Common.getLines("/users/yzcchen/chen3/eventBilingual/BilingualEvent/src/ACE_Chinese_train6");
		HashSet<String> annotated = new HashSet<String>();
		for(String line : activeLines) {
			String tks[] = line.split("\\s+");
			String file = tks[0];
			boolean all = false;
			for(int i=1;i<tks.length;i++) {
				if(tks[i].equals("all")) {
					all = true;
					break;
				}
			}
			if(all) {
				annotated.add(file);
			}
		}
		int annotatedDoc = 0;
		
		for (String line : lines) {
			if (docNo % 50 == 0) {
				System.out.println(docNo + "/" + lines.size());
			}
			CoNLLDocument d = ACEReader.read(line, true);
			d.language = "chinese";
			int a = line.indexOf("annotations");
			a += "annotations/".length();
			int b = line.lastIndexOf(".");
			String docName = line.substring(a, b);

			ArrayList<EntityMention> entityMentions = allEntityMentions
					.get(line);

			for (CoNLLPart part : d.getParts()) {
				for (EntityMention m : entityMentions) {
					m.head = part.rawText.substring(m.headCharStart,
							m.headCharEnd + 1);
					ACECorefCommon.assingStartEnd(m, part);
					em.EMUtil.setMentionAttri(m, part);
				}
				if(annotated.contains(line)) {
					annotatedDoc += 1;
					groups.addAll(extractGroups(part, docName, part.goldMentions));
				} else {
					groups.addAll(extractGroups(part, docName, entityMentions));
				}
			}
			docNo++;
		}
		System.out.println("annotatedDoc: " + annotatedDoc);
	}

	static int percent = 10;

	public static HashMap<String, HashMap<String, String>> corefResults = new HashMap<String, HashMap<String, String>>();

	public static HashMap<String, ArrayList<String>> corefProbs = new HashMap<String, ArrayList<String>>();
	
	public static void estep(ArrayList<ResolveGroup> groups) {
		for (ResolveGroup group : groups) {
			String docID = group.part.getDocument().getDocumentID();
			
			HashMap<String, String> corefResult = corefResults.get(docID);
			ArrayList<String> corefProb = corefProbs.get(docID);

			if(corefResult==null) {
				corefResult = new HashMap<String, String>();
				corefProb = new ArrayList<String>();
				corefResults.put(docID, corefResult);
				corefProbs.put(docID, corefProb);
			}
			
			String pronoun = group.pronoun;
			double norm = 0;
			for (Entry entry : group.entries) {
				String ant = entry.head;
				Context context = entry.context;
				double p_person = 0;
				if (entry.sameSpeaker) {
					p_person = personP.getVal(entry.person.name(), EMUtil
							.getPerson(pronoun).name());
				} else {
					p_person = personQP.getVal(entry.person.name(), EMUtil
							.getPerson(pronoun).name());
				}
				double p_number = numberP.getVal(entry.number.name(), EMUtil
						.getNumber(pronoun).name());
				double p_gender = genderP.getVal(entry.gender.name(), EMUtil
						.getGender(pronoun).name());
				double p_animacy = animacyP.getVal(entry.animacy.name(), EMUtil
						.getAnimacy(pronoun).name());

				double p_context = 1;

				if (fracContextCount.containsKey(context.toString())) {
					p_context = (EMUtil.alpha + fracContextCount.get(context
							.toString()))
							/ (2 * EMUtil.alpha + contextPrior.get(context
									.toString()));
				} else {
					// p_context = 1.0 / 2592.0;
					p_context = 1.0 / 2;
				}
				double p_context_l1 = pl1;
				double p_context_l0 = pl0;

//				for (int i = 0; i < Context.getSubContext().size(); i++) {
//					String key = context.getKey(i);
//					if (key.equals("-")) {
//						System.out.println(context.toString());
//						Common.bangErrorPOS("!!!");
//					}
//					if (multiFracContextsProbl1.get(i).containsKey(key)) {
//						p_context_l1 *= multiFracContextsProbl1.get(i).get(key);
//					} else {
//						p_context_l1 *= 1.0 / Context.normConstant.get(i);
//					}
//
//					if (multiFracContextsProbl0.get(i).containsKey(key)) {
//						p_context_l0 *= multiFracContextsProbl0.get(i).get(key);
//					} else {
//						p_context_l0 *= 1.0 / Context.normConstant.get(i);
//					}
//				}
//				p_context = p_context_l1 / (p_context_l1 + p_context_l0);

				entry.p = 1 * p_person * p_number * p_gender * p_animacy
						* p_context * 1;
				norm += entry.p;
			}

			double maxp = -1;
			String antName = null;
			for (Entry entry : group.entries) {
				entry.p = entry.p / norm;
				if(entry.p>maxp) {
					maxp = entry.p;
					antName = entry.name;
				}
				String key = group.part.getDocument().getFilePath() + " " + entry.name + " " + group.name + " " + entry.p;
				corefProb.add(key);
			}
			corefResult.put(group.name, antName);
		}
	}

	public static void mstep(ArrayList<ResolveGroup> groups) {
		genderP.resetCounts();
		numberP.resetCounts();
		animacyP.resetCounts();
		personP.resetCounts();
		personQP.resetCounts();
		fracContextCount.clear();
		
		for (int i = 0; i < multiFracContextsCountl1.size(); i++) {
			multiFracContextsCountl0.get(i).clear();
			multiFracContextsCountl1.get(i).clear();
			multiFracContextsProbl0.get(i).clear();
			multiFracContextsProbl1.get(i).clear();
		}

		for (ResolveGroup group : groups) {
			String pronoun = group.pronoun;

			for (Entry entry : group.entries) {
				double p = entry.p;
				String ant = entry.head;
				Context context = entry.context;
				numberP.addFracCount(entry.number.name(),
						EMUtil.getNumber(pronoun).name(), p);
				genderP.addFracCount(entry.gender.name(),
						EMUtil.getGender(pronoun).name(), p);
				animacyP.addFracCount(entry.animacy.name(),
						EMUtil.getAnimacy(pronoun).name(), p);

				if (entry.sameSpeaker) {
					personP.addFracCount(entry.person.name(),
							EMUtil.getPerson(pronoun).name(), p);
				} else {
					personQP.addFracCount(entry.person.name(), EMUtil
							.getPerson(pronoun).name(), p);
				}

				Double d = fracContextCount.get(context.toString());
				if (d == null) {
					fracContextCount.put(context.toString(), p);
				} else {
					fracContextCount.put(context.toString(), d.doubleValue()
							+ p);
				}
				
				for (int i = 0; i < Context.getSubContext().size(); i++) {
					int ps[] = Context.getSubContext().get(i);
					String key = context.getKey(i);
					double l1 = p;
					double l0 = 1 - p;

					Double cl0 = multiFracContextsCountl0.get(i).get(key);
					if (cl0 == null) {
						multiFracContextsCountl0.get(i).put(key, l0);
					} else {
						multiFracContextsCountl0.get(i).put(key,
								l0 + cl0.doubleValue());
					}

					Double cl1 = multiFracContextsCountl1.get(i).get(key);
					if (cl1 == null) {
						multiFracContextsCountl1.get(i).put(key, l1);
					} else {
						multiFracContextsCountl1.get(i).put(key,
								l1 + cl1.doubleValue());
					}
				}
				
			}
		}
		
		for (int i = 0; i < Context.getSubContext().size(); i++) {

			for (String key : multiFracContextsCountl1.get(i).keySet()) {

				double contextcountl0 = 1;
				if (multiFracContextsCountl0.get(i).containsKey(key)) {
					contextcountl0 += multiFracContextsCountl0.get(i).get(key);
				}
				double pcountl0 = contextcountl0
						/ (countl0 + Context.normConstant.get(i));

				double contextcountl1 = 1;
				if (multiFracContextsCountl1.get(i).containsKey(key)) {
					contextcountl1 += multiFracContextsCountl1.get(i).get(key);
				}
				double pcountl1 = contextcountl1
						/ (countl1 + Context.normConstant.get(i));

				multiFracContextsProbl0.get(i).put(key, pcountl0);
				multiFracContextsProbl1.get(i).put(key, pcountl1);
			}
		}
		
		genderP.setVals();
		numberP.setVals();
		animacyP.setVals();
		personP.setVals();
		personQP.setVals();
	}

	public static void main(String args[]) throws Exception {
		Util.part = args[0];
		percent = 10;
		run();
	}

	public static void run() throws IOException, FileNotFoundException {
		init();

		EMUtil.train = true;

		ArrayList<ResolveGroup> groups = new ArrayList<ResolveGroup>();
		extractCoNLL(groups);

		HashMap<String, Double> map = new HashMap<String, Double>();
		for (ResolveGroup rg : groups) {
			String pr = rg.pronoun;
			Double i = map.get(pr);
			if (i == null) {
				map.put(pr, 1.0);
			} else {
				map.put(pr, i.doubleValue() + 1.0);
			}
		}
		for (String key : map.keySet()) {
			System.out.println(key + ":" + map.get(key) + "=" + map.get(key)
					/ groups.size());
		}

		int it = 0;
		while (it < 20) {
			System.out.println("Iteration: " + it);
			estep(groups);
			mstep(groups);
			it++;
		}

		numberP.printParameter("numberP");
		genderP.printParameter("genderP");
		animacyP.printParameter("animacyP");
		personP.printParameter("personP");
		personQP.printParameter("personQP");

		ObjectOutputStream modelOut = new ObjectOutputStream(
				new FileOutputStream("EMModelPronoun"));
		modelOut.writeObject(numberP);
		modelOut.writeObject(genderP);
		modelOut.writeObject(animacyP);
		modelOut.writeObject(personP);
		modelOut.writeObject(personQP);

		modelOut.writeObject(multiFracContextsProbl0);
		modelOut.writeObject(multiFracContextsProbl1);
		modelOut.writeObject(pl0);
		modelOut.writeObject(pl1);
		
		modelOut.writeObject(fracContextCount);
		modelOut.writeObject(contextPrior);
		modelOut.writeObject(Context.ss);
		modelOut.writeObject(Context.vs);
		
		modelOut.close();

		// ApplyPronounEM.run(Util.part);

	}

}
