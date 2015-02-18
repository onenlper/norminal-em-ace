package util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import model.Entity;
import model.EntityMention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import em.EMUtil;

public class Inspect2 {

	public static void main(String args[]) {
		ArrayList<String> lines = Common.getLines("chinese_list_all_test");
		lines.addAll(Common.getLines("chinese_list_all_development"));
		lines.addAll(Common.getLines("chinese_list_all_train"));
		
		double all = 0;
		double ana = 0;
		int exc = 0;
		int gap = 0;
		for (String line : lines) {
			CoNLLDocument d = new CoNLLDocument(line.replace("auto_conll", "gold_conll"));
			for(CoNLLPart part : d.getParts()) {
				
				HashSet<String> goldNEs = EMUtil.getGoldNEs(part);
				HashSet<String> goldPNs = EMUtil.getGoldPNs(part);
				
				ArrayList<EntityMention> allNPs = EMUtil.extractMention(part);
				
				HashSet<String> allCommonNPs = new HashSet<String>();
				for(EntityMention m : allNPs) {
					if (goldPNs.contains(m.toName())) {
						continue;
					}
					if(goldNEs.contains(m.toName())
							|| goldNEs.contains(m.end + "," + m.end)
							) {
						continue;
					}
					allCommonNPs.add(m.toName());	
				}
				
				ArrayList<Entity> chains = part.getChains();
				HashMap<String, HashSet<String>> goldAnaCommonNPs = EMUtil.getGoldAnaphorKeys(chains, part);
				int a = allCommonNPs.size();
//				allCommonNPs.addAll(goldAnaCommonNPs.keySet());
				int b = allCommonNPs.size();
//				System.out.println((b - a) + "#");
				gap += b - a;
//				HashSet<String> gold = new HashSet<String>(goldAnaCommonNPs.keySet());
//				gold.removeAll(allCommonNPs);
				for(String k : allCommonNPs) {
					if (goldAnaCommonNPs.containsKey(k)) {
						ana += 1;
					}
				}
				
//				for(String key : goldAnaCommonNPs.keySet()) {
//					if(!allCommonNPs.contains(key)) {
//						int k = key.indexOf(",");
//						int start = Integer.parseInt(key.substring(0, k));
//						int end = Integer.parseInt(key.substring(k+1));
//						StringBuilder sb = new StringBuilder();
//						for(int i=start;i<=end;i++) {
//							sb.append(part.getWord(i).word);
//						}
//						System.out.println(key + " # " + sb.toString() + " # " + line);
//						exc ++;
//						
//					}
//				}
//				ana += goldAnaCommonNPs.size();
				all += allCommonNPs.size();
			}
		}
		System.out.println("Percent: " + ana/all);
		System.out.println("anaphoric: " + ana);
		System.out.println("all: " + all);
		System.out.println("Exception: " + exc);
		
		System.out.println("Gap: " + gap);
	}
}
