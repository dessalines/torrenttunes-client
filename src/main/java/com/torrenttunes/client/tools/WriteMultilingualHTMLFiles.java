package com.torrenttunes.client.tools;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

public class WriteMultilingualHTMLFiles {

	public static void write() {
		new WriteMultilingualHTMLFiles();
	}

	private WriteMultilingualHTMLFiles() {

		writeForLanguageMap(Strings.EN, DataSources.MAIN_PAGE_URL_EN());
		writeForLanguageMap(Strings.ES, DataSources.MAIN_PAGE_URL_ES());

	}
	
	private static void writeForLanguageMap(Strings language, String outputFile) {
		try {
		
			/*
			 * There are unfortunately two layers of mustache. The first is the strings,
			 * {{strings.blah}}, and the second is within the scripts, like {{artist.mbid}}
			 * 
			 * Your main.template file changed all the {{artist.mbid}} to @@artist.mbid~~ , 
			 * because otherwise this first language pass will fuck up all the others too,
			 * so after this first pass, you change the back to {{artist.mbid}}
			 * 
			 * { to @
			 * } to ~
			 */
			
			
			Reader reader = new FileReader(new File(DataSources.HTML_TEMPLATE_LOCATION()));

			File file = new File(outputFile);
			if (!file.exists()) file.createNewFile();
				
			Writer writer = new FileWriter(file);
			
			MustacheFactory mf = new DefaultMustacheFactory();
			Mustache mustache = mf.compile(reader, "example");
			mustache.execute(writer, language.map);
			writer.flush();
			
			// Now replace all leftover mustache back to correct {{artist.mbid}}
			String text = Tools.readFile(outputFile);
			text = text.replaceAll("@", "{").replaceAll("~", "}");
			
			Tools.writeFile(text, outputFile);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


}
