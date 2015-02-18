package supervised;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import model.EntityMention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import util.Common;
import em.EMUtil;

public class SupervisedAnaphor2 {

	public static void train() {
		ArrayList<String> lines = Common.getLines("chinese_list_all_train");
		lines.addAll(Common.getLines("chinese_list_all_development"));
		ArrayList<String> svmLines = new ArrayList<String>();
		AnaphorFea anaFea = new AnaphorFea(true, "anaphor");
		for (String line : lines) {
			CoNLLDocument d = new CoNLLDocument(line.replace("auto_conll",
					"gold_conll"));
			for (CoNLLPart part : d.getParts()) {
				HashSet<String> goldNEs = EMUtil.getGoldNEs(part);
				ArrayList<EntityMention> goldBoundaryNPMentions = EMUtil
						.extractMention(part);
				Collections.sort(goldBoundaryNPMentions);

				ArrayList<EntityMention> mentions = new ArrayList<EntityMention>();
				for (EntityMention m : goldBoundaryNPMentions) {
					if (m.start == m.end
							&& part.getWord(m.start).posTag.equals("PN")) {
						continue;
					}
					mentions.add(m);
				}
				Collections.sort(mentions);

				HashMap<String, HashSet<String>> goldAnaphorNouns = EMUtil
						.getGoldAnaphorKeys(part.getChains(), part);

				for (int i = 0; i < mentions.size(); i++) {
					EntityMention anaphor = mentions.get(i);
					if (goldNEs.contains(anaphor.toName())) {
						continue;
					}
					ArrayList<EntityMention> cands = new ArrayList<EntityMention>(
							mentions.subList(0, i));
					String fea = createInstance(anaphor, cands, part,
							goldAnaphorNouns.containsKey(anaphor.toName()), anaFea);
					svmLines.add(fea);
				}
			}
		}
		Common.outputLines(svmLines, "svm.anaphor.train");
		anaFea.freeze();
	}

	public static void test() {
		EMUtil.loadPredictNE("all", "test");
		ArrayList<String> lines = Common.getLines("chinese_list_all_test");
		AnaphorFea anaFea = new AnaphorFea(false, "anaphor");
		ArrayList<String> allMs = new ArrayList<String>();

		ArrayList<String> svms = new ArrayList<String>();
		for (String line : lines) {
			CoNLLDocument document = new CoNLLDocument(line);
			for (int k = 0; k < document.getParts().size(); k++) {
				CoNLLPart part = document.getParts().get(k);

				part.setNameEntities(EMUtil.predictNEs.get(part.getDocument()
						.getDocumentID() + "_" + part.getPartID()));

				CoNLLPart goldPart = EMUtil.getGoldPart(part, "test");
				HashMap<String, HashSet<String>> goldAnaphorNouns = EMUtil
						.getGoldAnaphorKeys(goldPart.getChains(), goldPart);
				HashSet<String> goldPNs = EMUtil.getGoldPNs(goldPart);
				HashSet<String> goldNEs = EMUtil.getGoldNEs(goldPart);

				ArrayList<EntityMention> goldBoundaryNPMentions = EMUtil
						.extractMention(part);
				Collections.sort(goldBoundaryNPMentions);

				ArrayList<EntityMention> candidates = new ArrayList<EntityMention>();
				for (EntityMention m : goldBoundaryNPMentions) {
					if (m.start == m.end
							&& part.getWord(m.start).posTag.equals("PN")) {
						continue;
					}
					candidates.add(m);
				}

				Collections.sort(candidates);

				for (EntityMention anaphor : goldBoundaryNPMentions) {
					if (anaphor.start == anaphor.end
							&& part.getWord(anaphor.end).posTag.equals("PN")) {
						continue;
					}
					if (goldNEs.contains(anaphor.toName())) {
						continue;
					}

					ArrayList<EntityMention> cands = new ArrayList<EntityMention>();
					for (int h = candidates.size() - 1; h >= 0; h--) {
						EntityMention cand = candidates.get(h);
						cand.sentenceID = part.getWord(cand.start).sentence
								.getSentenceIdx();
						cand.s = part.getWord(cand.start).sentence;
						if (cand.start < anaphor.start
								&& cand.end != anaphor.end) {
							cands.add(cand);
						}
					}

					String key = part.getPartName() + ":" + anaphor.toName();
					allMs.add(key);
					String fea = createInstance(anaphor, cands, part,
							goldAnaphorNouns.containsKey(anaphor.toName()), anaFea);
					svms.add(fea);
				}
			}
		}
		System.out.println(all);
		System.out.println(pos);
		System.out.println(pos / all);
		Common.outputLines(svms, "svm.anaphor.test");
		Common.outputLines(allMs, "allMs");
	}
	
	public static void main(String args[]) {
		train();
		test();
	}

	static double all = 0;
	static double pos = 0;

	public static String createInstance(EntityMention anaphor,
			ArrayList<EntityMention> cands, CoNLLPart part, boolean label, AnaphorFea fea) {
		all++;
		if (label) {
			pos += 1;
		}

		fea.configure(anaphor, cands, part);

		String svmStr = fea.getSVMFormatString();
		if (label) {
			return "+1 " + svmStr;
		} else {
			return "-1 " + svmStr;
		}
	}
}
