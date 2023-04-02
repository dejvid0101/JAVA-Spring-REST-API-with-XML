package me.iis.server.classes;

import com.thaiopensource.validate.SchemaReader;
import com.thaiopensource.validate.ValidationDriver;
import com.thaiopensource.validate.auto.AutoSchemaReader;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.FileNotFoundException;

import static org.springframework.util.ResourceUtils.getFile;

public class MyXMLValidator {
    public Validator InitXSDValidator(String XSDfilepath) throws SAXException, FileNotFoundException {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Source schemaFile = new StreamSource(getFile(XSDfilepath));
        Schema schema = factory.newSchema(schemaFile);
        return schema.newValidator();
    }

    public void JAXBvalidateXSD(String XSDfilepath, String XMLfilepath) throws JAXBException, SAXException {
        JAXBContext jaxbContext;


            //Get JAXBContext
            jaxbContext = JAXBContext.newInstance(Weather_Collection.class);

            //Create Unmarshaller
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

            //Setup schema validator
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema employeeSchema = sf.newSchema(new File(XSDfilepath));
            jaxbUnmarshaller.setSchema(employeeSchema);

            //Unmarshal xml file
            Weather_Collection XMLRoot = (Weather_Collection) jaxbUnmarshaller.unmarshal(new File(XMLfilepath));

    }

    public void ValidateRNG(String XMLfile, String RNGfilepath) throws Exception {
        SchemaReader sr = new AutoSchemaReader();
        ValidationDriver driver = new ValidationDriver(sr);
        InputSource inRng = ValidationDriver.fileInputSource(RNGfilepath);
        inRng.setEncoding("UTF-8");
        Boolean success=driver.loadSchema(inRng);
        //if loadSchema returns false, something went wrong
        if (!success){
            //exception handled by ServerApplication
            throw new Exception();
        }
        InputSource inXml = ValidationDriver.fileInputSource(XMLfile);
        inXml.setEncoding("UTF-8");
        success=driver.validate(inXml);
        //if validate returns false, xml isn't valid
        if (!success){
            //exception handled by ServerApplication
            throw new Exception();
        }

    }
}
