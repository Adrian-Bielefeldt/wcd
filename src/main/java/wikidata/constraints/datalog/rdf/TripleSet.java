package wikidata.constraints.datalog.rdf;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocumentProcessor;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.PropertyDocument;
import org.wikidata.wdtk.datamodel.interfaces.Reference;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;

import wikidata.constraints.datalog.main.Main;

public class TripleSet implements EntityDocumentProcessor {
	
	static final Logger logger = LoggerFactory.getLogger(TripleSet.class);
	
	protected String property;
	
	protected Map<String, String> qualifiers;
	
	protected static String BASE_LOCATION = "./resources/tripleSets/";
	
	File tripleSetFile;
	
	File qualifierTripleSetFile;
	
	File referenceTripleSetFile;
	
	BufferedWriter writer;
	
	BufferedWriter referenceWriter;
	
	BufferedWriter qualifierWriter;
	
	public TripleSet(String property_, Map<String, String> quualifiers_) throws IOException {
		property = property_;
		qualifiers = quualifiers_;
		
		tripleSetFile = new File(BASE_LOCATION + getTripleSetType() + "/" + property + ".csv");
		tripleSetFile.getParentFile().mkdirs();
		tripleSetFile.createNewFile();
		qualifierTripleSetFile = new File(BASE_LOCATION + getTripleSetType() + "/" + property + "_qualifier" + ".csv");
		qualifierTripleSetFile.getParentFile().mkdirs();
		qualifierTripleSetFile.createNewFile();
		referenceTripleSetFile = new File(BASE_LOCATION + getTripleSetType() + "/" + property + "_reference" + ".csv");
		referenceTripleSetFile.getParentFile().mkdirs();
		referenceTripleSetFile.createNewFile();
		
		writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tripleSetFile, false)));
		qualifierWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(qualifierTripleSetFile, false)));
		referenceWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(referenceTripleSetFile, false)));
		
		Main.registerProcessor(this);
	}
	
	protected final void write(String id, String subject, String predicate, String object) {
		try {
			writer.append(listToCSV(Arrays.asList(id, subject, predicate, object)));
			writer.newLine();
		} catch (IOException e) {
			logger.error("Could not write line to file " + tripleSetFile.getAbsolutePath(), e);
		}
		
	}	
	
	protected final void writeQualifier(String id, String predicate, String object) {
		try {
			qualifierWriter.write(listToCSV(Arrays.asList(id, predicate, object)));
			qualifierWriter.newLine();
		} catch (IOException e) {
			logger.error("Could not write line to file " + referenceTripleSetFile.getAbsolutePath(), e);
		}
		
	}
	
	protected final void writeReference(String id, String predicate, String object) {
		try {
			referenceWriter.write(listToCSV(Arrays.asList(id, predicate, object)));
			referenceWriter.newLine();
		} catch (IOException e) {
			logger.error("Could not write line to file " + qualifierTripleSetFile.getAbsolutePath(), e);
		}
		
	}
	
	protected String getTripleSetType() {
		return "base";
	}
	
	public File getTripleSetFile() throws IOException {
		writer.close();
		return tripleSetFile;
	}
	
	public File getReferenceTripleSetFile() throws IOException {
		referenceWriter.close();
		return referenceTripleSetFile;
	}
	
	public File getQualifierTripleSetFile() throws IOException {
		qualifierWriter.close();
		return qualifierTripleSetFile;
	}
	
	protected void triple(String id, String subject, String predicate, String object) {
		write(id, subject, predicate, object);
	}
	
	protected void qualifier(String id, String predicate, String object) {
		writeQualifier(id, predicate, object);
	}
	
	protected void reference(String id, String predicate, String object) {
		writeReference(id, predicate, object);
	}
	
	@Override
	public void processItemDocument(ItemDocument itemDocument) {
		String subject = itemDocument.getEntityId().getIri();
		for (StatementGroup	sg : itemDocument.getStatementGroups()) {
			String predicate = sg.getProperty().getIri();
			for (Statement statement : sg) {
				String id = statement.getStatementId();
				String object = statement.getValue().accept(new OutputValueVisitor());
				triple(id, subject, predicate, object);
				
				for (SnakGroup qualifier : statement.getClaim().getQualifiers()) {
					String qualifier_predicate = qualifier.getProperty().getIri();
					for (Snak snak : qualifier) {
						String qualifier_object = snak.getValue().accept(new OutputValueVisitor());
						qualifier(id, qualifier_predicate, qualifier_object);
					}
				}
				for (Reference reference : statement.getReferences()) {
					for (SnakGroup rGroup : reference.getSnakGroups()) {
						String reference_predicate = rGroup.getProperty().getIri();
						for (Snak snak : rGroup) {
							String reference_object = snak.getValue().accept(new OutputValueVisitor());
							reference(id, reference_predicate, reference_object);
						}
					}
				}
			}
		}		
	}
	
	@Override
	public void processPropertyDocument(PropertyDocument propertyDocument) {
		
	}
	
	public void close() throws IOException {
		writer.close();
		qualifierWriter.close();
		referenceWriter.close();
	}
	
	protected static String listToCSV(List<String> strings) {
		String result = "";
		for (String string : strings) {
			result += string + ",";
		}
		result = result.substring(0, result.length() - 1);
		return result;
	}

}
