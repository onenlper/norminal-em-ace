package em;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import model.Element;
import model.Entity;
import model.EntityMention;
import model.EntityMention.Animacy;
import model.EntityMention.Gender;
import model.EntityMention.Number;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import pronounEM.EMPronounLearnSeed;
import util.Common;
import util.Util;
import ace.ACECorefCommon;
import ace.reader.ACEReader;
import em.ResolveGroup.Entry;

public class EMLearnSeed {

	// static HashMap<Context, Double> p_context_ = new HashMap<Context,
	// Double>();

	static Parameter numberP;
	static Parameter genderP;
	static Parameter semanticP;

	static Parameter grammaticP;

	static Parameter animacyP;
	static Parameter cilin;

	static double word2vecSimi = .9;

	static HashMap<String, Double> contextPrior;
	static HashMap<String, Double> contextOverall;
	static HashMap<String, Double> fracContextCount;

	static HashMap<String, Double> contextVals;

	static int maxDistance = 50000;
	// static int maxDistance = 75;

	static int maxDisFeaValue = 10;
	// static int contextSize = 2 * 2 * 2 * 3 * 2 * (maxDisFeaValue + 1);
	public static int qid = 0;

	// static int count = 0;

	public static void init() {
		// static HashMap<Context, Double> p_context_ = new HashMap<Context,
		// Double>();
		numberP = new Parameter(1.0 / ((double) Number.values().length));
		genderP = new Parameter(1.0 / ((double) Gender.values().length));
		semanticP = new Parameter(1.0 / 25318.0);

		// semanticP = new Parameter(1.0/5254.0);

		cilin = new Parameter(1.0 / 7089.0);

		grammaticP = new Parameter(1.0 / 4.0);

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
		contextVals = new HashMap<String, Double>();
		qid = 0;
		Context.contextCache.clear();
	}

	private static ArrayList<Element> getChGoldNE(CoNLLPart part) {
		String documentID = "/users/yzcchen/chen3/CoNLL/conll-2012/v4/data/train/data/chinese/annotations/"
				+ part.docName + ".v4_gold_skel";
		// System.out.println(documentID);
		CoNLLDocument document = new CoNLLDocument(documentID);
		CoNLLPart goldPart = document.getParts().get(part.getPartID());
		// for (Element ner : goldPart.getNameEntities()) {
		// int start = ner.start;
		// int end = ner.end;
		// String ne = ner.content;
		//
		// StringBuilder sb = new StringBuilder();
		// for (int k = start; k <= end; k++) {
		// sb.append(part.getWord(k).word).append(" ");
		// }
		// // System.out.println(sb.toString() + " # " + ne);
		// // System.out.println(goldPart.);
		// }
		return goldPart.getNameEntities();
	}

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

	@SuppressWarnings("unused")
	public static ArrayList<ResolveGroup> extractGroups(CoNLLPart part,
			String docName, ArrayList<EntityMention> entityMentions) {
		CoNLLPart goldPart = part;

		for (Entity e : part.getChains()) {
			for (EntityMention m : e.mentions) {
				EMUtil.setMentionAttri(m, part);
			}
		}

		ArrayList<ResolveGroup> groups = new ArrayList<ResolveGroup>();
		for (int i = 0; i < part.getCoNLLSentences().size(); i++) {
			CoNLLSentence s = part.getCoNLLSentences().get(i);

			s.mentions = extractMention(entityMentions, s);
			EMUtil.assignNE(s.mentions, part.getNameEntities());

			ArrayList<EntityMention> precedMs = new ArrayList<EntityMention>();

			for (int j = maxDistance; j >= 1; j--) {
				if (i - j >= 0) {
					for (EntityMention m : part.getCoNLLSentences().get(i - j).mentions) {
						if (pronounEM.EMUtil.pronouns.contains(m.head)) {
							continue;
						}
						precedMs.add(m);
					}
				}
			}
			Collections.sort(s.mentions);
			for (int j = 0; j < s.mentions.size(); j++) {
				EntityMention m = s.mentions.get(j);
				if (pronounEM.EMUtil.pronouns.contains(m.head)) {
					continue;
				}

				qid++;

				ArrayList<EntityMention> ants = new ArrayList<EntityMention>();
				ants.addAll(precedMs);

				if (j > 0) {
					for (EntityMention precedM : s.mentions.subList(0, j)) {
						if (pronounEM.EMUtil.pronouns.contains(precedM.head)
								|| precedM.end == m.end) {
							continue;
						}
						ants.add(precedM);
					}
				}

				EntityMention fake = new EntityMention();
				fake.extent = "fake";
				fake.head = "fake";
				fake.isFake = true;
				ants.add(fake);

				ResolveGroup rg = new ResolveGroup(m, part, ants);
				Collections.sort(ants);
				Collections.reverse(ants);

				int seq = 0;

				for (EntityMention ant : ants) {
					Entry entry = new Entry(ant, null, part);
					rg.entries.add(entry);
					entry.p_c = EMUtil.getP_C(ant, m, part);
					if (entry.p_c != 0) {
						seq += 1;
					}
				}
				for (Entry entry : rg.entries) {
					if (entry.isFake) {
						entry.p_c = Entry.p_fake_decay
								/ (Entry.p_fake_decay + seq);
					} else if (entry.p_c != 0) {
						entry.p_c = 1 / (Entry.p_fake_decay + seq);
					}
				}

				countl0 += rg.entries.size() - 1;
				countl1 += 1;

				pl0 = countl0 / (countl0 + countl1);
				pl1 = countl1 / (countl0 + countl1);

				groups.add(rg);
			}
		}
		return groups;
	}

	static double countl0 = 0;
	static double countl1 = 0;

	static double pl0 = 0;
	static double pl1 = 0;

	public static void sortEntries(ResolveGroup rg,
			HashMap<String, HashSet<String>> chainMaps) {
		ArrayList<Entry> goodEntries = new ArrayList<Entry>();
		ArrayList<Entry> fakeEntries = new ArrayList<Entry>();
		ArrayList<Entry> badEntries = new ArrayList<Entry>();
		for (int k = 0; k < rg.entries.size(); k++) {
			Entry entry = rg.entries.get(k);
			EntityMention ant = rg.entries.get(k).ant;

			// TODO
			if (entry.isFake) {
				fakeEntries.add(entry);
			} else if ((ant.head.equals(rg.m.head))
					|| EMUtil.isCopular(ant, rg.m, rg.part)
					|| EMUtil.isRoleAppositive(ant, rg.m)
					|| EMUtil.isAbbreviation(ant, rg.m)
					|| EMUtil.isSamePerson(ant, rg.m)) {
				goodEntries.add(entry);
			} else {
				badEntries.add(entry);
			}
		}
		ArrayList<Entry> allEntries = new ArrayList<Entry>();
		allEntries.addAll(goodEntries);
		allEntries.addAll(fakeEntries);
		allEntries.addAll(badEntries);
		for (int k = 0; k < allEntries.size(); k++) {
			allEntries.get(k).seq = k;
		}
	}

	static int percent = 10;

	public static ArrayList<String> types = new ArrayList<String>(
			Arrays.asList("wea", "veh", "per", "fac", "gpe", "loc", "org",
					"time", "val", "none"));

	public static HashMap<String, ArrayList<EntityMention>> loadSVMResult(
			String part) {
		HashMap<String, ArrayList<EntityMention>> entityMentionses = new HashMap<String, ArrayList<EntityMention>>();

		String folder = "/users/yzcchen/chen3/eventBilingual/BilingualEvent/src/";
		ArrayList<String> mentionStrs = Common.getLines(folder + "mention.test"
				+ part);
		System.out.println(mentionStrs.size());
		ArrayList<String> typeResult = Common.getLines(folder
				+ "multiType.result" + part);

		for (int i = 0; i < mentionStrs.size(); i++) {
			String mentionStr = mentionStrs.get(i);
			String fileKey = mentionStr.split("\\s+")[1];
			String startEndStr = mentionStr.split("\\s+")[0];
			int headStart = Integer.valueOf(startEndStr.split(",")[0]);
			int headEnd = Integer.valueOf(startEndStr.split(",")[1]);
			EntityMention em = new EntityMention();
			em.headCharStart = headStart;
			em.headCharEnd = headEnd;

			int typeIndex = Integer.valueOf(typeResult.get(i).split("\\s+")[0]);

			String type = types.get(typeIndex - 1);
			if (type.equalsIgnoreCase("none")
			// || type.equalsIgnoreCase("time")
			// || type.equalsIgnoreCase("val")
			) {
				continue;
			}

			em.semClass = type;

			ArrayList<EntityMention> mentions = entityMentionses.get(fileKey);
			if (mentions == null) {
				mentions = new ArrayList<EntityMention>();
				entityMentionses.put(fileKey, mentions);
			}
			if (type.equalsIgnoreCase("val")) {
				em.type = "Value";
			} else if (type.equalsIgnoreCase("time")) {
				em.type = "Time";
			} else {
				mentions.add(em);
			}
		}
		return entityMentionses;
	}

	private static void extractCoNLL(ArrayList<ResolveGroup> groups) {

		ArrayList<String> lines = Common
				.getLines("/users/yzcchen/chen3/eventBilingual/BilingualEvent/src/ACE_Chinese_train"
						+ Util.part);

		HashMap<String, ArrayList<EntityMention>> allEntityMentions = loadSVMResult("6");

		ArrayList<String> activeLines = Common
				.getLines("/users/yzcchen/chen3/eventBilingual/BilingualEvent/src/ACE_Chinese_train6");
		HashSet<String> annotated = new HashSet<String>();
		for (String line : activeLines) {
			String tks[] = line.split("\\s+");
			String file = tks[0];
			boolean all = false;
			for (int i = 1; i < tks.length; i++) {
				if (tks[i].equals("all")) {
					all = true;
					break;
				}
			}
			if (all) {
				annotated.add(file);
			}
		}
		int annotatedDoc = 0;
		for (int j = 0; j < lines.size(); j++) {
			String line = lines.get(j);
			CoNLLDocument d = ACEReader.read(line, true);
			d.setDocumentID(Integer.toString(j));

			d.setFilePath(line);

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
					EMUtil.setMentionAttri(m, part);
				}
				if (annotated.contains(line)) {
					annotatedDoc += 1;
					groups.addAll(extractGroups(part, docName,
							part.goldMentions));
				} else {
					groups.addAll(extractGroups(part, docName, entityMentions));
				}
			}
		}
		System.out.println("annotatedDoc: " + annotatedDoc);
	}

	// private static void extractGigaword(ArrayList<ResolveGroup> groups)
	// throws Exception {
	//
	// String folder = "/users/yzcchen/chen3/zeroEM/parser/";
	// int j = 0;
	// ArrayList<String> fns = new ArrayList<String>();
	// for (File subFolder : (new File(folder)).listFiles()) {
	// if (subFolder.isDirectory()
	// // && !subFolder.getName().contains("cna")
	// ) {
	// for (File file : subFolder.listFiles()) {
	// if (file.getName().endsWith(".text")) {
	// String filename = file.getAbsolutePath();
	// fns.add(filename);
	// }
	// }
	// }
	// }
	//
	// for (String filename : fns) {
	// System.out.println(filename + " " + (j++));
	// System.out.println(groups.size());
	// BufferedReader br = new BufferedReader(new FileReader(filename));
	// CoNLLPart part = new CoNLLPart();
	// int wID = 0;
	// String line = "";
	// while ((line = br.readLine()) != null) {
	// if (line.trim().isEmpty()) {
	// // part.setDocument(doc);
	// // doc.getParts().add(part);
	// part.wordCount = wID;
	// part.processDocDiscourse();
	//
	// // for(CoNLLSentence s : part.getCoNLLSentences()) {
	// // for(CoNLLWord w : s.getWords()) {
	// // if(!w.speaker.equals("-") &&
	// // !w.speaker.startsWith("PER")) {
	// // System.out.println(w.speaker);
	// // }
	// // }
	// // }
	// groups.addAll(extractGroups(part));
	// part = new CoNLLPart();
	// wID = 0;
	// continue;
	// }
	// MyTree tree = Common.constructTree(line);
	// CoNLLSentence s = new CoNLLSentence();
	// part.addSentence(s);
	// s.setStartWordIdx(wID);
	// s.syntaxTree = tree;
	// ArrayList<MyTreeNode> leaves = tree.leaves;
	// for (int i = 0; i < leaves.size(); i++) {
	// MyTreeNode leaf = leaves.get(i);
	// CoNLLWord word = new CoNLLWord();
	// word.orig = leaf.value;
	// word.word = leaf.value;
	// word.sentence = s;
	// word.indexInSentence = i;
	// word.index = wID++;
	// word.posTag = leaf.parent.value;
	//
	// // find speaker
	// word.speaker = "-";
	//
	// s.addWord(word);
	// }
	// s.setEndWordIdx(wID - 1);
	// }
	// part.processDocDiscourse();
	// groups.addAll(extractGroups(part));
	// br.close();
	// }
	// }
	public static HashMap<String, HashSet<String>> chainMaps = new HashMap<String, HashSet<String>>();

	public static HashMap<String, ArrayList<ArrayList<String>>> corefResults = new HashMap<String, ArrayList<ArrayList<String>>>();

	public static HashMap<String, ArrayList<String>> corefProbs = new HashMap<String, ArrayList<String>>();

	static ArrayList<HashMap<String, Double>> multiFracContextsCountl0 = new ArrayList<HashMap<String, Double>>();
	static ArrayList<HashMap<String, Double>> multiFracContextsCountl1 = new ArrayList<HashMap<String, Double>>();

	static ArrayList<HashMap<String, Double>> multiFracContextsProbl0 = new ArrayList<HashMap<String, Double>>();
	static ArrayList<HashMap<String, Double>> multiFracContextsProbl1 = new ArrayList<HashMap<String, Double>>();

	public static void estep(ArrayList<ResolveGroup> groups) {
		// System.out.println("estep starts:");
		long t1 = System.currentTimeMillis();
		chainMaps.clear();
		contextPrior.clear();
		corefProbs.clear();
		corefResults.clear();
		
		for (ResolveGroup rg : groups) {
			String docID = rg.part.getDocument().getDocumentID();

			ArrayList<ArrayList<String>> corefResult = corefResults.get(docID);
			ArrayList<String> corefProb = corefProbs.get(docID);

			if (corefResult == null) {
				corefResult = new ArrayList<ArrayList<String>>();
				corefProb = new ArrayList<String>();
				corefResults.put(docID, corefResult);
				corefProbs.put(docID, corefProb);
			}

			for (Entry entry : rg.entries) {
				if (!chainMaps.containsKey(entry.antName) && !entry.isFake) {
					HashSet<String> set = new HashSet<String>();
					set.add(entry.antName);
					chainMaps.put(entry.antName, set);
				}
			}
			sortEntries(rg, chainMaps);
			for (int k = 0; k < rg.entries.size(); k++) {
				Entry entry = rg.entries.get(k);
				// add antecedents
				entry.context = Context.buildContext(entry.ant, rg.m, rg.part,
						rg.ants, entry.seq);

				Double d = contextPrior.get(entry.context.toString());
				if (d == null) {
					contextPrior.put(entry.context.toString(), 1.0);
				} else {
					contextPrior.put(entry.context.toString(),
							1.0 + d.doubleValue());
				}
			}
			double norm = 0;
			for (Entry entry : rg.entries) {
				Context context = entry.context;

				double p_semetic = semanticP.getVal(entry.sem, rg.sem);

				double p_context = .5;

				double p_context_l1 = pl1;
				double p_context_l0 = pl0;

				for (int i = 0; i < Context.getSubContext().size(); i++) {
					String key = context.getKey(i);
					if (key.equals("-")) {
						System.out.println(context.toString());
						Common.bangErrorPOS("!!!");
					}
					if (multiFracContextsProbl1.get(i).containsKey(key)) {
						p_context_l1 *= multiFracContextsProbl1.get(i).get(key);
					} else {
						p_context_l1 *= 1.0 / Context.normConstant.get(i);
					}

					if (multiFracContextsProbl0.get(i).containsKey(key)) {
						p_context_l0 *= multiFracContextsProbl0.get(i).get(key);
					} else {
						p_context_l0 *= 1.0 / Context.normConstant.get(i);
					}
				}
				p_context = p_context_l1 / (p_context_l1 + p_context_l0);

				entry.p = p_context * entry.p_c;
				entry.p *= 1 * p_semetic;

				norm += entry.p;
			}

			double max = 0;
			double maxP = -1;
			int maxIdx = -1;
			String antName = "";

			EntityMention ant = null;
			if (norm != 0) {
				for (Entry entry : rg.entries) {
					entry.p = entry.p / norm;
					if (entry.p > max) {
						max = entry.p;
						antName = entry.antName;
						entry.ant.toCharName();
						ant = entry.ant;
					}
					if (!entry.ant.isFake) {
						String key = rg.part.getDocument().getFilePath() + " "
								+ entry.ant.toCharName() + " "
								+ rg.m.toCharName() + " " + entry.p;
						corefProb.add(key);
					}
				}
			} else {
				// Common.bangErrorPOS("!");
			}
			if (ant != null && !ant.isFake) {
				boolean find = false;
				out: for (int i = 0; i < corefResult.size(); i++) {
					ArrayList<String> entity = corefResult.get(i);
					for (String m : entity) {
						if (m.equalsIgnoreCase(ant.toCharName())) {
							entity.add(rg.m.toCharName());
							find = true;
							break out;
						}
					}
				}
				if (!find) {
					Common.bangErrorPOS("");
				}
			} else {
				ArrayList<String> entity = new ArrayList<String>();
				entity.add(rg.m.toCharName());
				corefResult.add(entity);
			}

			if (!antName.equals("fake") && !antName.isEmpty()) {
				HashSet<String> corefs = chainMaps.get(antName);
				corefs.add(rg.anaphorName);
				chainMaps.put(rg.anaphorName, corefs);
			}
		}
		// System.out.println(System.currentTimeMillis() - t1);
	}

	public static void mstep(ArrayList<ResolveGroup> groups) {
		// System.out.println("mstep starts:");
		long t1 = System.currentTimeMillis();
		genderP.resetCounts();
		numberP.resetCounts();
		animacyP.resetCounts();
		contextVals.clear();
		semanticP.resetCounts();
		grammaticP.resetCounts();
		cilin.resetCounts();
		fracContextCount.clear();

		for (int i = 0; i < multiFracContextsCountl1.size(); i++) {
			multiFracContextsCountl0.get(i).clear();
			multiFracContextsCountl1.get(i).clear();
			multiFracContextsProbl0.get(i).clear();
			multiFracContextsProbl1.get(i).clear();
		}

		for (ResolveGroup group : groups) {
			for (Entry entry : group.entries) {
				double p = entry.p;
				Context context = entry.context;

				numberP.addFracCount(entry.number.name(), group.number.name(),
						p);
				genderP.addFracCount(entry.gender.name(), group.gender.name(),
						p);
				animacyP.addFracCount(entry.animacy.name(),
						group.animacy.name(), p);

				semanticP.addFracCount(entry.sem, group.sem, p);

				grammaticP
						.addFracCount(entry.gram.name(), group.gram.name(), p);

				cilin.addFracCount(entry.cilin, group.cilin, p);

//				Double d = fracContextCount.get(context.toString());
//				if (d == null) {
//					fracContextCount.put(context.toString(), p);
//				} else {
//					fracContextCount.put(context.toString(), d.doubleValue()
//							+ p);
//				}

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
		genderP.setVals();
		numberP.setVals();
		animacyP.setVals();
		semanticP.setVals();
		cilin.setVals();
		grammaticP.setVals();
//		for (String key : fracContextCount.keySet()) {
//			double p_context = (EMUtil.alpha + fracContextCount.get(key))
//					/ (2.0 * EMUtil.alpha + contextPrior.get(key));
//			contextVals.put(key, p_context);
//		}

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
		// System.out.println(System.currentTimeMillis() - t1);
	}

	public static void main(String args[]) throws Exception {
		// EMUtil.loadAlign();
		Util.part = args[0];
		run();
		// System.out.println(match/XallX);
		// Common.outputLines(svmRanks, "svmRank.train");
		// System.out.println("Qid: " + qid);
	}

	private static void run() throws IOException, FileNotFoundException {
		init();

		EMUtil.train = true;

		ArrayList<ResolveGroup> groups = new ArrayList<ResolveGroup>();

		extractCoNLL(groups);
		// extractGigaword(groups);
		// Common.pause("count:  " + count);
		// Common.pause(groups.size());

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
		semanticP.printParameter("semanticP");
		grammaticP.printParameter("grammaticP");
		cilin.printParameter("cilinP");

		ObjectOutputStream modelOut = new ObjectOutputStream(
				new FileOutputStream("EMModel"));
		modelOut.writeObject(numberP);
		modelOut.writeObject(genderP);
		modelOut.writeObject(animacyP);
		modelOut.writeObject(semanticP);
		modelOut.writeObject(grammaticP);
		modelOut.writeObject(cilin);
//		modelOut.writeObject(fracContextCount);
//		modelOut.writeObject(contextPrior);
		
		modelOut.writeObject(multiFracContextsProbl0);
		modelOut.writeObject(multiFracContextsProbl1);
		modelOut.writeObject(pl0);
		modelOut.writeObject(pl1);

//		modelOut.writeObject(Context.ss);
//		modelOut.writeObject(Context.vs);
		// modelOut.writeObject(Context.svoStat);

		modelOut.close();

		Common.outputHashMap(contextVals, "contextVals");
		Common.outputHashMap(fracContextCount, "fracContextCount");
		Common.outputHashMap(contextPrior, "contextPrior");
		// ObjectOutputStream svoStat = new ObjectOutputStream(new
		// FileOutputStream(
		// "/dev/shm/svoStat"));
		// svoStat.writeObject(Context.svoStat);
		// svoStat.close();

		// System.out.println(EMUtil.missed);
		System.out.println(EMUtil.missed.size());

		EMPronounLearnSeed.run();

		ArrayList<String> files = Common
				.getLines("/users/yzcchen/chen3/eventBilingual/BilingualEvent/src/ACE_Chinese_train"
						+ Util.part);
		ArrayList<String> allCorefProbs = new ArrayList<String>();
		for (int i = 0; i < files.size(); i++) {
			allCorefProbs.addAll(corefProbs.get(Integer.toString(i)));
			if (EMPronounLearnSeed.corefProbs.containsKey(Integer.toString(i))) {
				allCorefProbs.addAll(EMPronounLearnSeed.corefProbs.get(Integer
						.toString(i)));
			}

			ArrayList<ArrayList<String>> nounCoref = corefResults.get(Integer
					.toString(i));
			HashMap<String, String> pronounCoref = EMPronounLearnSeed.corefResults
					.get(Integer.toString(i));

			if (pronounCoref != null) {
				for (String pro : pronounCoref.keySet()) {
					String ant = pronounCoref.get(pro);
					if (ant == null) {
						ArrayList<String> entity = new ArrayList<String>();
						entity.add(pro);
						nounCoref.add(entity);
					} else {
						boolean find = false;
						out: for (int j = 0; j < nounCoref.size(); j++) {
							ArrayList<String> entity = nounCoref.get(j);
							for (String m : entity) {
								if (m.equals(ant)) {
									find = true;
									entity.add(pro);
									break out;
								}
							}
						}
						if (!find) {
							ArrayList<String> e = new ArrayList<String>();
							e.add(ant);
							e.add(pro);
							nounCoref.add(e);
						}
					}
				}
			}
			ArrayList<String> lines = new ArrayList<String>();
			for (ArrayList<String> entity : nounCoref) {
				StringBuilder sb = new StringBuilder();
				for (String m : entity) {
					sb.append(m).append(" ");
				}
				lines.add(sb.toString().trim());
			}
			Common.outputLines(lines, "em_coref_train" + Util.part + "/" + i
					+ ".entity.coref");
		}

		Common.outputLines(allCorefProbs, "entityCorefProbTrain" + Util.part);
		//
		ApplyEMSeed.run(Util.part);
		// ApplyEM.run("nw");
		// ApplyEM.run("mz");
		// ApplyEM.run("wb");
		// ApplyEM.run("bn");
		// ApplyEM.run("bc");
		// ApplyEM.run("tc");
	}

}
