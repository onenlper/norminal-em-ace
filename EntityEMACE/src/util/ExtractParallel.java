package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import model.Entity;
import model.EntityMention;

import org.xml.sax.InputSource;

public class ExtractParallel {

	public static void main(String args[]) throws Exception{
//		extract1();
//		extract2();
		extract3();
	}
	
	
	public static void extract3() throws Exception {
		String fold1 = "/users/yzcchen/corpora/LDC2009T11/data/chinese_source/ctb/timex2norm/";
		String fold2 = "/users/yzcchen/corpora/LDC2009T11/data/chinese_source/nw/timex2norm/";
		String fold3 = "/users/yzcchen/corpora/LDC2009T11/data/chinese_source/wl/timex2norm/";
		
		ArrayList<String> folds = new ArrayList<String>();
		folds.add(fold1);
		folds.add(fold2);
		folds.add(fold3);
		ArrayList<String> typeOutput = new ArrayList<String>();
		ArrayList<String> subtypeOutput = new ArrayList<String>();
		for(String fold : folds) {
			for(String fn : (new File(fold)).list()) {
				if(!fn.endsWith(".apf.xml") || fn.endsWith("eng.apf.xml") || fn.endsWith("arb.apf.xml")) {
					continue;
				}
				String apfFn = fold + fn;
				ArrayList<Entity> entities = new ArrayList<Entity>();
				try {
					InputStream inputStream = new FileInputStream(new File(apfFn));
					SAXParserFactory sf = SAXParserFactory.newInstance();
					SAXParser sp = sf.newSAXParser();
					APFXMLReader reader = new APFXMLReader(entities);
					sp.parse(new InputSource(inputStream), reader);
					
					for(Entity e : entities) {
						for(EntityMention m : e.mentions) {
							String extent = m.extent.replace("\n", "").replace("\r", "").replaceAll("\\s+", "");
							String head = m.head.replace("\n", "").replace("\r", "").replaceAll("\\s+", "");
							System.out.println(extent + " # " + head + " # " + m.semClass + " # " + m.subType);
							
							typeOutput.add(head + " " + m.semClass);
							subtypeOutput.add(extent + " " + m.subType);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		Common.outputLines(typeOutput, "semanticTypes2");
		Common.outputLines(subtypeOutput, "subTypes2");
	}
	
	private static void extract2() throws Exception {
		ArrayList<String> output = new ArrayList<String>();
		ArrayList<String> lines = Common.getLines("dict/dict1");
		for(String line : lines) {
			if(line.trim().isEmpty() || line.startsWith("#")) {
				continue;
			}
			String tks[] = line.replaceAll("\\([^\\)]*\\)", "").replaceAll("\\[[^\\]]*\\]", "").split("\\t");
			String chis[] = tks[0].split(";");
			String engs[] = tks[1].split(";");

			for(String chi : chis) {
				for(String eng : engs) {
					output.add(chi.trim() + "#####" + eng.trim());
				}
			}
		}
		
		lines = Common.getLines("dict/dict2");
		for(String line : lines) {
			if(line.trim().isEmpty() || line.startsWith("#")) {
				continue;
			}
			String tks[] = line.replaceAll("\\([^\\)]*\\)", "").replaceAll("\\[[^\\]]*\\]", "").split("\\t");
			
			String chis[] = tks[0].split(",");
			String engs[] = tks[1].split(",");

			for(String chi : chis) {
				for(String eng : engs) {
					String key = chi.trim() + "#####" + eng.trim();
					output.add(key);
				}
			}
		}
		
		lines = Common.getLines("dict/dict3");
		for(String line : lines) {
			if(line.trim().isEmpty() || line.startsWith("#")) {
				continue;
			}
			String tks[] = line.replaceAll("\\([^\\)]*\\)", "").replaceAll("\\[[^\\]]*\\]", "").split("\\t");
			String chis[] = tks[0].split(";");
			String engs[] = tks[1].split(";");
//
			for(String chi : chis) {
				for(String eng : engs) {
					String key = chi.trim() + "#####" + eng.trim();
					output.add(key);
				}
			}
		}
		
		lines = Common.getLines("dict/dict4");
		for(String line : lines) {
			if(line.trim().isEmpty() || line.startsWith("#")) {
				continue;
			}
			if(line.startsWith("Category:") || line.startsWith("category:")) {
				line = line.substring("Category:".length());
			}
			
			
			String tks[] = line.replaceAll("\\([^\\)]*\\)", "").replaceAll("\\[[^\\]]*\\]", "").split("\\t");

			String chis[] = tks[0].split(";");
			String engs[] = tks[1].split(";");
			for(String chi : chis) {
				for(String eng : engs) {
					String key = chi.trim() + "#####" + eng.trim();
					output.add(key);
				}
			}
		}
		Common.outputLines(output, "chi_eng.trans2");
	}

	private static void extract1() throws IOException {
		String fold = "/shared/mlrdir3/disk1/mlr/corpora/LDC2005T34/ch_eng_ent_lists/data/text/";
//		String ces[] = {"ldc_orgs_intl_ce_v1.beta.txt", "ldc_propernames_industry_ce_v1.beta.txt", "ldc_propernames_org_ce_v1.beta.txt",
//				"ldc_propernames_other_ce_v1.beta.txt", "ldc_propernames_people_ce_v1.beta.txt", "ldc_propernames_place_ce_v1.beta.txt",
//				"ldc_propernames_press_ce_v1.beta.txt", "ldc_whoswho_china_ce_v1.txt", "ldc_whoswho_international_ce_v1.txt"};
//		
//		String ecs[] = {"ldc_orgs_intl_ec_v1.beta.txt", "ldc_propernames_industry_ec_v1.beta.txt", "ldc_propernames_org_ec_v1.beta.txt",
//				"ldc_propernames_other_ec_v1.beta.txt", "ldc_propernames_people_ec_v1.beta.txt", "ldc_propernames_place_ec_v1.beta.txt",
//				"ldc_propernames_press_ec_v1.beta.txt", "ldc_whoswho_china_ec_v1.txt", "ldc_whoswho_international_ec_v1.txt"};
		
		ArrayList<String> output = new ArrayList<String>();
		
		for(String file : (new File(fold)).list()) {
			if(!file.contains("_ce_") && !file.contains("_ec_")) {
				continue;
			}
			
			BufferedReader br = Common.getBufferedReader2(fold + file);
			String line = "";
			while((line=br.readLine())!=null) {
				String chi = "";
				String eng = "";
				if(file.equals("ldc_orgs_intl_ce_v1.beta.txt") || file.equals("ldc_propernames_industry_ce_v1.beta.txt")
				|| file.equals("ldc_propernames_org_ce_v1.beta.txt") || file.equals("ldc_propernames_other_ce_v1.beta.txt")
				|| file.equals("ldc_propernames_people_ce_v1.beta.txt") || file.equals("ldc_propernames_place_ce_v1.beta.txt")
				|| file.equals("ldc_propernames_press_ce_v1.beta.txt")) {
					int a = line.indexOf("/");
					int b = line.lastIndexOf("/");
					chi = line.substring(0, a).trim();
					eng = line.substring(a+1, b).trim(); 			
				} else if(file.equals("ldc_whoswho_china_ce_v1.txt")) {
					int a = line.indexOf("\t");
					int b = line.lastIndexOf("\t");
					if(a!=b) {
						chi = line.substring(0, a).trim();
						eng = line.substring(a+1, b).trim();
					} else {
						System.out.println(line);
						continue;
					}
				} else if(file.equals("ldc_whoswho_international_ce_v1.txt") ) {
					int a = line.indexOf("\t");
					int c = line.indexOf('\t', a+1);
					chi = line.substring(0, a).trim();
					eng = line.substring(a+1, c).trim();
//					System.out.println(chi);
//					System.out.println(eng);
				} else if(file.equals("ldc_orgs_intl_ec_v1.beta.txt") || file.equals("ldc_whoswho_china_ec_v1.txt")) {
					int a = line.indexOf("\t");
					int b = line.lastIndexOf("\t");
					if(a!=b) {
						eng = line.substring(0, a).trim();
						chi = line.substring(a+1, b).trim();
					} else {
						System.out.println(line);
						continue;
					}
				} else if(file.equals("ldc_propernames_industry_ec_v1.beta.txt") || file.equals("ldc_propernames_org_ec_v1.beta.txt")
						|| file.equals("ldc_propernames_other_ec_v1.beta.txt") || file.equals("ldc_propernames_people_ec_v1.beta.txt")
						|| file.equals("ldc_propernames_place_ec_v1.beta.txt") || file.equals("ldc_propernames_press_ec_v1.beta.txt")) {
					int a = line.indexOf("/");
					int b = line.lastIndexOf("/");
					if(a!=b) {
						eng = line.substring(0, a).trim();
						chi = line.substring(a+1, b).trim();
					} else {
						System.out.println(line);
						continue;
					}
				} else if(file.equals("ldc_whoswho_international_ec_v1.txt")) {
					int a = line.indexOf("\t");
					int c = line.indexOf('\t', a+1);
					eng = line.substring(0, a).trim();
					chi = line.substring(a+1, c).trim();
//					System.out.println(chi + "##" + eng);
				}
				
				if(eng.contains(",")) {
//					System.out.println(line);
				}
					
				output.add(chi + "#####" + eng);
			}
		}
		Common.outputLines(output, "chi_eng.trans");
	}
}
