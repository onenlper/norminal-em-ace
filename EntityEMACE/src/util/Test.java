//package util;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.HashSet;
//
//import model.Entity;
//import model.EntityMention;
//import model.CoNLL.CoNLLDocument;
//import model.CoNLL.CoNLLPart;
//import em.EMUtil;
//
//public class Test {
//	public static void main(String args[]) {
//		// ArrayList<String> lines = Common.getLines("chinese_list_all_train");
//		// lines.addAll(Common.getLines("chinese_list_all_development"));
//
//		// ArrayList<String> lines = Common.getLines("chinese_list_all_test");
//		//
//		// int s = 0;
//		// for (String line : lines) {
//		// CoNLLDocument d = new CoNLLDocument(line
//		// // .replace("gold_conll", "auto_conll")
//		// .replace("auto_conll", "gold_conll"));
//		// for(CoNLLPart p : d.getParts()) {
//		// s += EMUtil.getGoldAnaphorKeys(p.getChains(), p).size();
//		// }
//		// }
//		// System.out.println(s);
//		// ArrayList<String> lines = Common.getLines("yago/prepositions");
//		// ArrayList<String> output = new ArrayList<String>();
//		// for(String l : lines) {
//		// if(l.trim().isEmpty()) {
//		// continue;
//		// }
//		// output.add(l.trim());
//		// }
//		// Common.outputLines(output, "prep");
//		EMUtil.loadAlign();
//		double allAnaM = 0;
//		double hitAnaM = 0;
//		ArrayList<String> lines = Common.getLines("chinese_list_all_test");
//		for (String line : lines) {
//			CoNLLDocument d = new CoNLLDocument(line.replace("auto_conll",
//					"gold_conll"));
//			d.language = "chinese";
//			int a = line.indexOf("annotations");
//			a += "annotations/".length();
//			int b = line.lastIndexOf(".");
//			String docName = line.substring(a, b);
//			
//			for (CoNLLPart part : d.getParts()) {
//				CoNLLPart goldPart = EMUtil.getGoldPart(part, "test");
//				HashMap<String, String> goldNEs = EMUtil.getGoldNEs2(goldPart);
//				HashSet<String> goldPNs = EMUtil.getGoldPNs(goldPart);
//				
//				ArrayList<Entity> chains = goldPart.getChains();
//				
//				for(Entity e : chains) {
//					for(EntityMention m : e.mentions) {
//						m.s = goldPart.getWord(m.end).sentence;
//						ArrayList<EntityMention> lst = new ArrayList<EntityMention>();
//						lst.add(m);
//						EMUtil.alignMentions(m.s, lst, docName);
//					}
//				}
//				
//				for(Entity entity : chains) {
//					ArrayList<EntityMention> mentions = entity.mentions;
//					Collections.sort(mentions);
//				
//					for(int i=0;i<mentions.size();i++) {
//						EntityMention m2 = mentions.get(i);
//						String h2 = m2.extent.split("\\s+")[m2.extent.split("\\s+").length-1];
//						if(goldNEs.containsKey(m2.toName()) || goldPNs.contains(m2.toName()) || goldNEs.containsKey(m2.end + "," + m2.end)) {
//							continue;
//						}
//						boolean hit = false;
//						for(int j=i-1;j>=0;j--) {
//							EntityMention m1 = mentions.get(j);
//							String h1 = m1.extent.split("\\s+")[m1.extent.split("\\s+").length-1];
//							if(goldPNs.contains(m1.toName())) {
//								continue;
//							}
//							
//							if(m1.extent.contains(m2.extent) || m2.extent.contains(m1.extent)) {
//								continue;
//							}
//							
//							if(goldNEs.containsKey(m1.toName()) || goldNEs.containsKey(m1.end + "," + m1.end)) {
//								EntityMention xm1 = m1.getXSpan();
//								EntityMention xm2 = m2.getXSpan();
//								System.out.println(m1.extent + "<-" + m2.extent);
//								System.out.println((xm1==null?"null":xm1.extent) + "<-" + (xm2==null?"null":xm2.extent));
//								System.out.println(goldNEs.get(m1.toName()) + " ### " + goldNEs.get(m1.end + "," + m1.end));
//								System.out.println("===============");
//								hit = true;
//							}
//							if(h1.contains(h2)) {
//								hit = false;
//								break;
//							}
//						}
//						if(hit) {
//							hitAnaM += 1;
//						}
//						for(int j=i-1;j>=0;j--) {
//							EntityMention m1 = mentions.get(j);
//							if(goldPNs.contains(m1.toName())) {
//								continue;
//							}
//							allAnaM += 1;
//							break;
//						}
//						
//					}
//				}
//			}
//		}
//		System.out.println(hitAnaM + ":" + allAnaM + "=" + hitAnaM/allAnaM);
//	}
//}
