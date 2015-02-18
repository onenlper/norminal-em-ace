package util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import model.Entity;
import model.EntityMention;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.syntaxTree.MyTreeNode;
import ace.ACECommon;
import ace.ACECorefCommon;
import ace.PlainText;

public class Util {

	public static String part;

	public static boolean anaphorExtension = false;

	public static String modifyAlign = "/users/yzcchen/chen3/ijcnlp2013/sentenceAlign/senAlignOut_modify/";
	public static String originalAlign = "/users/yzcchen/chen3/ijcnlp2013/sentenceAlign/senAlignOut/";

	public static String tokenAlignBase = "/users/yzcchen/chen3/ijcnlp2013/wordAlign/tokenBase/";

	public static String headAlignBaseGold = "/users/yzcchen/chen3/ijcnlp2013/wordAlign/headBase_gold/";
	public static String headAlignBaseSys = "/users/yzcchen/chen3/ijcnlp2013/wordAlign/headBase_sys/";

	public static String headBAAlignBaseGold = "/users/yzcchen/chen3/ijcnlp2013/wordAlign/headBase_BA_gold/";
	public static String headBAAlignBaseSys = "/users/yzcchen/chen3/ijcnlp2013/wordAlign/headBase_BA_sys/";

	public static String tokenBAAlignBaseGold = "/users/yzcchen/chen3/ijcnlp2013/wordAlign/tokenBase_BA_gold/";
	public static String tokenBAAlignBaseSys = "/users/yzcchen/chen3/ijcnlp2013/wordAlign/tokenBase_BA_sys/";

	static String annotationsStr = "annotations/";

	public static String getID(String file) {
		int start = file.indexOf("annotations/") + annotationsStr.length();
		int dot = file.lastIndexOf(".");
		String id = file.substring(start, dot);
		return id;
	}

	private static String getTrainFile(String ID, String lang, boolean gold) {
		String base = "";
		if (lang.equalsIgnoreCase("chi")) {
			base = trainBase + "chinese/annotations/";
		} else if (lang.equalsIgnoreCase("eng")) {
			base = trainBase + "english/annotations/";
		}
		if (gold) {
			return base + ID + ".v4_gold_conll";
		} else {
			return base + ID + ".v4_auto_conll";
		}
	}

	private static String getDevFile(String ID, String lang, boolean gold) {
		String base = "";
		if (lang.equalsIgnoreCase("chi")) {
			base = devBase + "chinese/annotations/";
		} else if (lang.equalsIgnoreCase("eng")) {
			base = devBase + "english/annotations/";
		}
		if (gold) {
			return base + ID + ".v4_gold_conll";
		} else {
			return base + ID + ".v4_auto_conll";
		}
	}

	private static String getTestFile(String ID, String lang, boolean gold) {
		String base = "";
		if (lang.equalsIgnoreCase("chi")) {
			base = testBase + "chinese/annotations/";
		} else if (lang.equalsIgnoreCase("eng")) {
			base = testBase + "english/annotations/";
		}
		if (gold) {
			return base + ID + ".v4_gold_conll";
		} else {
			if (lang.equalsIgnoreCase("chi")) {
				return base + ID + ".v5_auto_conll";
			} else {
				return base + ID + ".v4_auto_conll";
			}
		}
	}

	static String devBase = "/users/yzcchen/chen3/CoNLL/conll-2012/v4/data/development/data/";
	static String trainBase = "/users/yzcchen/chen3/CoNLL/conll-2012/v4/data/train/data/";
	static String testBase = "/users/yzcchen/chen3/CoNLL/conll-2012/v4/data/test/data/";

	public static String getFullPath(String ID, String lang, boolean gold) {
		String trainFn = getTrainFile(ID, lang, gold);
		// System.out.println(trainFn);
		if ((new File(trainFn)).exists()) {
			return trainFn;
		}
		String devFn = getDevFile(ID, lang, gold);
		// System.out.println(devFn);
		if ((new File(devFn)).exists()) {
			return devFn;
		}
		String testFn = getTestFile(ID, lang, gold);
		// System.out.println(testFn);
		if ((new File(testFn)).exists()) {
			return testFn;
		}
		return null;
	}

	public static String getMTPath(int ID, String lang) {
		String base = "/users/yzcchen/chen3/ijcnlp2013/googleMTRED/" + lang
				+ "_MT/conll/";
		return base + ID + ".conll";
	}

	public static EntityMention formPhrase(MyTreeNode treeNode,
			CoNLLSentence sentence) {
		ArrayList<MyTreeNode> leaves = treeNode.getLeaves();
		int startIdx = leaves.get(0).leafIdx;
		int endIdx = leaves.get(leaves.size() - 1).leafIdx;
		int start = sentence.getWord(startIdx).index;
		int end = sentence.getWord(endIdx).index;
		StringBuilder sb = new StringBuilder();
		for (int i = startIdx; i <= endIdx; i++) {
			sb.append(sentence.getWord(i).word).append(" ");
		}
		EntityMention em = new EntityMention();
		em.start = start;
		em.end = end;
		em.extent = sb.toString().trim();
		return em;
	}

	public static ArrayList<EntityMention> getSieveCorefMentions(CoNLLPart doc,
			ArrayList<String> files, int fileIdx) {
		// /users/yzcchen/chen3/conll12/chinese/goldEntityMentions/
		String baseFolder = "/users/yzcchen/chen3/conll12/chinese/systemEntityMentions/ACE_test_"
				+ Util.part + "/";

		String os = System.getProperty("os.name");
		if (os.startsWith("Windows")) {
			baseFolder = "C:\\Users\\USER\\workspace\\BilingualEvent\\data\\ACE_test_"
					+ Util.part + "\\";
		}
		ArrayList<String> lines = Common.getLines(baseFolder + fileIdx	+ ".entities.sieve.entity");
		ArrayList<EntityMention> allMentions = new ArrayList<EntityMention>();

		ArrayList<Entity> entities = new ArrayList<Entity>();

		PlainText plainText = ACECommon.getPlainText(files.get(fileIdx) + ".sgm");

		for (String line : lines) {
			Entity entity = new Entity();
			String tokens[] = line.split("\\s+");
			for (String token : tokens) {
				String pos[] = token.split(",");
				EntityMention mention = new EntityMention();
				int charStart = Integer.valueOf(Integer.valueOf(pos[0]));
				int charEnd = Integer.valueOf(Integer.valueOf(pos[1]));
				mention.start = charStart;
				mention.end = charEnd;

				mention.head = plainText.content.substring(charStart, charEnd + 1).replaceAll("\\s+", "").replace("\n", "").replace("\r", "");
				mention.headCharStart = charStart;
				mention.headCharEnd = charEnd;
				
				assignSystemSemantic(mention, files.get(fileIdx));
				ACECorefCommon.assingStartEnd(mention, doc);
				allMentions.add(mention);
			}
			entities.add(entity);
		}
		// System.out.println(allMentions.size());
//		doc.setEntityCorefMap(entities);
		return allMentions;
	}
	
	static HashMap<String, ArrayList<EntityMention>> allSemanticResult;

	public static void assignSystemSemantic(EntityMention mention,
			String fileID) {
		if (allSemanticResult == null) {
			allSemanticResult = loadSemanticResult();
		}
		String stem = fileID.substring(fileID.indexOf("Chinese"));
		String key = "/users/yzcchen/ACL12/data/ACE2005/" + stem + ".sgm";
		key = key.replace("\\", "/");
		ArrayList<EntityMention> systems = allSemanticResult.get(key);
		
		boolean find = false;
		for (EntityMention system : systems) {
			if (system.headStart == mention.headCharStart
					&& system.headEnd == mention.headCharEnd) {
				mention.subType = system.subType;
				mention.semClass = system.semClass;
				find = true;
				break;
			}
		}
		if (!find) {
			System.err.println("GEE");
//			mention.subType = "other";
//			mention.semClass = "other";
			Common.bangErrorPOS("");
			System.exit(1);
		}
	}
	
	public static HashMap<String, ArrayList<EntityMention>> loadSemanticResult() {
		HashMap<String, ArrayList<EntityMention>> allSVMResult = new HashMap<String, ArrayList<EntityMention>>();
		// /users/yzcchen/chen3/conll12/chinese/semantic_gold_mention
		String folder = "/users/yzcchen/ACL12/model/ACE2005/semantic_system_mention/";
//		String folder = "/users/yzcchen/ACL12/model/ACE2005/semantic3/";
		
		String os = System.getProperty("os.name");
		if(os.startsWith("Windows")) {
			folder = "C:\\Users\\USER\\workspace\\BilingualEvent\\data\\semantic_system_mention\\";
		}
		ArrayList<String> mentionStrs = Common.getLines(folder + "mention.test"
				+ Util.part);
		ArrayList<String> typeResult = Common.getLines(folder
				+ "multiType.result2" + Util.part);
		ArrayList<String> subTypeResult = Common.getLines(folder
				+ "multiSubType.result2" + Util.part);

		for (int i = 0; i < mentionStrs.size(); i++) {
			String mentionStr = mentionStrs.get(i);
			String fileKey = mentionStr.split("\\s+")[1];
			String startEndStr = mentionStr.split("\\s+")[0];
			int headStart = Integer.valueOf(startEndStr.split(",")[0]);
			int headEnd = Integer.valueOf(startEndStr.split(",")[1]);
			EntityMention em = new EntityMention();
			em.headStart = headStart;
			em.headEnd = headEnd;
			em.start = headStart;
			em.end = headEnd;
			int typeIndex = Integer.valueOf(typeResult.get(i).split("\\s+")[0]);
			int subTypeIndex = Integer.valueOf(subTypeResult.get(i).split(
					"\\s+")[0]);

			em.semClass = semClasses.get(typeIndex - 1);
			em.subType = semSubTypes.get(subTypeIndex - 1);

			if (allSVMResult.containsKey(fileKey)) {
				allSVMResult.get(fileKey).add(em);
			} else {
				ArrayList<EntityMention> ems = new ArrayList<EntityMention>();
				ems.add(em);
				allSVMResult.put(fileKey, ems);
			}
		}
		return allSVMResult;
	}
	
	public static ArrayList<String> semClasses = new ArrayList<String>(
			Arrays.asList("wea", "veh", "per", "fac", "gpe", "loc", "org"));
	public static ArrayList<String> semSubTypes = new ArrayList<String>(
			Arrays.asList("f-airport", "f-building-grounds", "f-path",
					"f-plant", "f-subarea-facility", "g-continent",
					"g-county-or-district", "g-gpe-cluster", "g-nation",
					"g-population-center", "g-special", "g-state-or-province",
					"l-address", "l-boundary", "l-celestial",
					"l-land-region-natural", "l-region-general",
					"l-region-international", "l-water-body", "o-commercial",
					"o-educational", "o-entertainment", "o-government",
					"o-media", "o-medical-science", "o-non-governmental",
					"o-religious", "o-sports", "p-group", "p-indeterminate",
					"p-individual", "v-air", "v-land", "v-subarea-vehicle",
					"v-underspecified", "v-water", "w-biological", "w-blunt",
					"w-chemical", "w-exploding", "w-nuclear", "w-projectile",
					"w-sharp", "w-shooting", "w-underspecified", "o-other"));

	public static Set<String> reportVerb = new HashSet<String>(Arrays.asList(
			"accuse", "acknowledge", "add", "admit", "advise", "agree",
			"alert", "allege", "announce", "answer", "apologize", "argue",
			"ask", "assert", "assure", "beg", "blame", "boast", "caution",
			"charge", "cite", "claim", "clarify", "command", "comment",
			"compare", "complain", "concede", "conclude", "confirm",
			"confront", "congratulate", "contend", "contradict", "convey",
			"counter", "criticize", "debate", "decide", "declare", "defend",
			"demand", "demonstrate", "deny", "describe", "determine",
			"disagree", "disclose", "discount", "discover", "discuss",
			"dismiss", "dispute", "disregard", "doubt", "emphasize",
			"encourage", "endorse", "equate", "estimate", "expect", "explain",
			"express", "extoll", "fear", "feel", "find", "forbid", "forecast",
			"foretell", "forget", "gather", "guarantee", "guess", "hear",
			"hint", "hope", "illustrate", "imagine", "imply", "indicate",
			"inform", "insert", "insist", "instruct", "interpret", "interview",
			"invite", "issue", "justify", "learn", "maintain", "mean",
			"mention", "negotiate", "note", "observe", "offer", "oppose",
			"order", "persuade", "pledge", "point", "point out", "praise",
			"pray", "predict", "prefer", "present", "promise", "prompt",
			"propose", "protest", "prove", "provoke", "question", "quote",
			"raise", "rally", "read", "reaffirm", "realise", "realize",
			"rebut", "recall", "reckon", "recommend", "refer", "reflect",
			"refuse", "refute", "reiterate", "reject", "relate", "remark",
			"remember", "remind", "repeat", "reply", "report", "request",
			"respond", "restate", "reveal", "rule", "say", "see", "show",
			"signal", "sing", "slam", "speculate", "spoke", "spread", "state",
			"stipulate", "stress", "suggest", "support", "suppose", "surmise",
			"suspect", "swear", "teach", "tell", "testify", "think",
			"threaten", "told", "uncover", "underline", "underscore", "urge",
			"voice", "vow", "warn", "welcome", "wish", "wonder", "worry",
			"write", "表示", "讲起", "说话", "说", "指出", "介绍", "认为", "密布", "觉得", "汇给",
			"低吟", "想", "介绍", "以为", "惊叫", "回忆", "宣告", "报道", "透露", "谈", "感慨",
			"反映", "宣布", "指"));
}
