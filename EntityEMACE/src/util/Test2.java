package util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Test2 {
	
	public static void main(String args[]) throws Exception {
		
		String fn = "/shared/mlrdir3/disk1/mlr/corpora/LDC2005T34/ch_eng_ent_lists/data/text/ldc_whoswho_international_ec_v1.txt";
		String encode = resolveCode(fn);
		
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fn), encode));
		String line = "";
		while((line=br.readLine())!=null) {
//			String l2 = new String(line.getBytes("ISO-8859-1"), "utf-8");
			System.out.println(line);
//			System.out.println(l2);
		}
		br.close();
	}
	
	public static String resolveCode(String path) throws Exception {  
//      String filePath = "D:/article.txt"; //[-76, -85, -71]  ANSI  
//      String filePath = "D:/article111.txt";  //[-2, -1, 79] unicode big endian  
//      String filePath = "D:/article222.txt";  //[-1, -2, 32]  unicode  
//      String filePath = "D:/article333.txt";  //[-17, -69, -65] UTF-8  
        InputStream inputStream = new FileInputStream(path);    
        byte[] head = new byte[3];    
        inputStream.read(head);      
        String code = "gb2312";  //或GBK  
        if (head[0] == -1 && head[1] == -2 )    
            code = "UTF-16";    
        else if (head[0] == -2 && head[1] == -1 )    
            code = "Unicode";    
        else if(head[0]==-17 && head[1]==-69 && head[2] ==-65)    
            code = "UTF-8";    
            
        inputStream.close();  
          
        System.out.println(code);   
        return code;  
    }  
}
