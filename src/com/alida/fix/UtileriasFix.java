package com.alida.fix;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.*;


/**
 * Clase de utiliras, mapea los valores de la cadena en pares de valores
 * @author (Mejoras y ajustes) Gilberto Romero
 *
 */
public class UtileriasFix {

	private static MultiMap  valoresFIX = null;
	
	/**
	 * Mapea los valores de la cadena original en pares de valores
	 * @param cadena
	 * @return
	 * @throws Exception
	 */
	public static MultiMap  getValores(String cadena) throws Exception{
		
		try{
			System.out.println(" ++++++ UtileriasFix.getValores() +++++++++++++");
			valoresFIX = new MultiValueMap();
			String[] valores = cadena.split("\\|"); 
			//  
			System.out.println("valores:" + valores.length);
			for(int i = 0; i < valores.length   ; i++){
				String[] tagvalor = valores[i].split("=");
				System.out.println("tagvalor: " + tagvalor[0] + " = " + tagvalor[1].trim());
				valoresFIX.put(tagvalor[0], tagvalor[1].trim());
			}
		}catch(Exception e){throw new Exception("The received string has an error and the FIX message can not be generated.  String: " + cadena); }
		
		return valoresFIX;
	}
	
	
}
