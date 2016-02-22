import net.sourceforge.tess4j.Tesseract;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;


public class ocr_sign {

    private static final String TessdataPath = "./res/tessdata";
    private static final String pdfFilePath = "./res/SOMECOMPANY.pdf";

    public static HashMap<String, String> extractData(String source, ArrayList<String> keys) {
        HashMap<String, String> data = new HashMap<>();
        String temp;
        String tempsource = source;
        String tempvalue;
        do {
            temp = tempsource.substring(0,tempsource.indexOf("\n"));
            for (String key : keys) {
                if (temp.contains(key + ":")) {
                    tempvalue = temp.substring(temp.indexOf(":") + 2, temp.length()); //2 is a lazy way to remove a 1 space symbol :D
                    data.put(key, tempvalue);
                    System.out.println("Extracted " + key + " = " + tempvalue);
                }
            }
            tempsource = tempsource.substring(tempsource.indexOf("\n")+1);
        } while (tempsource.length() > 0);

        return data;
    }

    //All pdf splitting/rendering and ocr in main(looks really bad :D ) since i have no idea about how this will look like after merging into alfresco.
    public static void main(String[] args) {
        try {
            //
            File pdfFile = new File(pdfFilePath);
            System.out.println(pdfFile.exists());

            //
            PDDocument document = PDDocument.load(pdfFile);
            System.out.println("Pagecount = " + document.getNumberOfPages());

            //
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(0,600);

            /*
            //test image to check if everything went fine
            File TestOutput = new File ("./res/output.png");
            ImageIO.write(image, "png", TestOutput);
            */

            //OCR stuff with prepared image
            Tesseract instance = new Tesseract();
            instance.setDatapath(TessdataPath);
            instance.setLanguage("eng");
            //File testImage = new File("./res/1.png");
            String result1 = instance.doOCR(image);
            String result2 = instance.doOCR(image,new Rectangle(3202,2039,1280,106));

            //System.out.println(result1);
            //System.out.println(result2);

            ArrayList<String> keys = new ArrayList<>();
            keys.add("Due");
            keys.add("From");
            keys.add("To");

            HashMap<String, String> obtainedData = extractData(result1, keys);
            obtainedData.put("Total", result2.substring(result2.lastIndexOf(":")));


            //testing APIs for metadata
            //PDDocumentInformation metadata = document.getDocumentInformation();
            //metadata.setCustomMetadataValue("testkey", "testvalue");
            //metadata.setAuthor("Test Author");

            PDDocumentInformation metadata = document.getDocumentInformation();
            obtainedData.forEach((k,v) -> metadata.setCustomMetadataValue(k,v)); //
            document.setDocumentInformation(metadata);

            //writing new PDF to FS
            String name = pdfFile.getName();
            String outputName = name.substring(0, name.lastIndexOf(".")) + "_updated.pdf";
            File outputFile = new File(pdfFile.getParent(), outputName);
            document.save(outputFile);
            document.close();


            /*
            //Let's check that new pdf has that (testkey,testvalue)
            document.load(new File(pdfFileOutputPath));
            metadata = document.getDocumentInformation();
            System.out.println(metadata.getCustomMetadataValue("testkey"));
            document.close();
            */

            String[] arguments = {"/home/argentum/keystore.p12", "123456", outputFile.getCanonicalPath()};
            CreateSignature.main(arguments);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
