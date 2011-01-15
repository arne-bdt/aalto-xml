package async;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

public class BasicParsingTest extends AsyncTestBase
{
    public void testRootElement() throws Exception
    {
        // let's try with different chunking, addition (or not) of space
        for (int spaces = 0; spaces < 3; ++spaces) {
            String SPC = "  ".substring(0, spaces);
            _testEmptyRoot(1, SPC+"<root />");
            _testEmptyRoot(1, SPC+"<root/>");
            _testEmptyRoot(1, SPC+"<root></root>");
            _testEmptyRoot(2, SPC+"<root />");
            _testEmptyRoot(2, SPC+"<root/>");
            _testEmptyRoot(2, SPC+"<root></root>");
            _testEmptyRoot(3, SPC+"<root />");
            _testEmptyRoot(3, SPC+"<root/>");
            _testEmptyRoot(3, SPC+"<root></root>");
            _testEmptyRoot(5, SPC+"<root />");
            _testEmptyRoot(5, SPC+"<root/>");
            _testEmptyRoot(5, SPC+"<root></root>");
        }
    }

    public void testComments() throws Exception
    {
        for (int spaces = 0; spaces < 3; ++spaces) {
            String SPC = "  ".substring(0, spaces);
            _testComments(SPC, 1);
            _testComments(SPC, 2);
            _testComments(SPC, 3);
            _testComments(SPC, 5);
        }
    }

    public void testProcInstr() throws Exception
    {
        for (int spaces = 0; spaces < 3; ++spaces) {
            String SPC = "  ".substring(0, spaces);
            _testPI(SPC, 1);
            _testPI(SPC, 2);
            _testPI(SPC, 3);
            _testPI(SPC, 5);
        }
    }
    
    /*
    /**********************************************************************
    /* Secondary test methods
    /**********************************************************************
     */

    private void _testEmptyRoot(int chunkSize, String XML) throws Exception
    {
        AsyncXMLInputFactory f = new InputFactoryImpl();
        AsyncXMLStreamReader sr = f.createAsyncXMLStreamReader();
        AsyncReaderWrapper reader = new AsyncReaderWrapper(sr, chunkSize, XML);

        // should start with START_DOCUMENT, but for now skip
        int t = reader.nextToken();
        assertTokenType(START_ELEMENT, t);
        assertEquals("root", sr.getLocalName());
        assertEquals("", sr.getNamespaceURI());
        assertEquals(0, sr.getAttributeCount());
        assertTokenType(END_ELEMENT, reader.nextToken());
        assertEquals("root", sr.getLocalName());
        assertEquals("", sr.getNamespaceURI());
        assertTokenType(XMLStreamConstants.END_DOCUMENT, reader.nextToken());
        assertFalse(sr.hasNext());
    }

    private void _testComments(String spaces, int chunkSize) throws Exception
    {
        String XML = spaces+"<!--comments&s\r\ntuf-fy>--><root><!----></root><!--\nHi - ho!->-->";
        AsyncXMLInputFactory f = new InputFactoryImpl();
        AsyncXMLStreamReader sr = f.createAsyncXMLStreamReader();
        AsyncReaderWrapper reader = new AsyncReaderWrapper(sr, chunkSize, XML);
        int t = _verifyStart(reader);
        assertTokenType(COMMENT, t);
        assertEquals("comments&s\ntuf-fy>", sr.getText());
        assertTokenType(START_ELEMENT, reader.nextToken());
        assertEquals("root", sr.getLocalName());
        assertTokenType(COMMENT, reader.nextToken());
        assertEquals("", sr.getText());
        assertTokenType(END_ELEMENT, reader.nextToken());
        assertEquals("root", sr.getLocalName());
        assertTokenType(COMMENT, reader.nextToken());
        assertEquals("\nHi - ho!", sr.getText());
        assertTokenType(END_DOCUMENT, reader.nextToken());
    }

    private void _testPI(String spaces, int chunkSize) throws Exception
    {
        String XML = spaces+"<?p    i ?><root><?pi \nwith\r\ndata? or not????></root><?proc    \r?>";
        AsyncXMLInputFactory f = new InputFactoryImpl();
        AsyncXMLStreamReader sr = f.createAsyncXMLStreamReader();
        AsyncReaderWrapper reader = new AsyncReaderWrapper(sr, chunkSize, XML);
        int t = _verifyStart(reader);
        assertTokenType(PROCESSING_INSTRUCTION, t);
        assertEquals("p", sr.getPITarget());
        assertEquals("i ", sr.getPIData());
        assertTokenType(START_ELEMENT, reader.nextToken());
        assertEquals("root", sr.getLocalName());
        assertTokenType(PROCESSING_INSTRUCTION, reader.nextToken());
        assertEquals("pi", sr.getPITarget());
        assertEquals("with\ndata? or not???", sr.getPIData());
        assertTokenType(END_ELEMENT, reader.nextToken());
        assertEquals("root", sr.getLocalName());
        assertTokenType(PROCESSING_INSTRUCTION, reader.nextToken());
        assertEquals("proc", sr.getPITarget());
        assertEquals("", sr.getPIData());
        assertTokenType(END_DOCUMENT, reader.nextToken());
    }

    private void _testSimple(int chunkSize) throws Exception
    {
        AsyncXMLInputFactory f = new InputFactoryImpl();
        AsyncXMLStreamReader sr = f.createAsyncXMLStreamReader();
        AsyncReaderWrapper reader = new AsyncReaderWrapper(sr, chunkSize, 
            "<!--comment&s\r\ntuf-fy>--><root\r\nattr='1' >text<![CDATA[cdata\r\n&] ]] stuff]]></root><?pi\r\ndata? what\ndata??>");

        // minor deviation from Stax; START_DOCUMENT not available right away
//        assertTokenType(AsyncXMLStreamReader.EVENT_INCOMPLETE, reader.currentToken());
        int t = reader.nextToken();
//        assertTokenType(XMLStreamConstants.START_DOCUMENT, reader.nextToken());
        assertTokenType(XMLStreamConstants.COMMENT, t);
        assertEquals("comment&s\ntuf-fy>", sr.getText());
        assertTokenType(START_ELEMENT, reader.nextToken());
        assertEquals("root", sr.getLocalName());
        assertEquals("", sr.getNamespaceURI());
        assertEquals(1, sr.getAttributeCount());
        assertEquals("attr", sr.getAttributeLocalName(0));
        assertEquals("", sr.getAttributeNamespace(0));
        assertEquals("1", sr.getAttributeValue(0));
        assertTokenType(CHARACTERS, reader.nextToken());
        String str = collectAsyncText(reader, CHARACTERS); // moves to end-element
        assertEquals("text", str);
        // note: moved to next element by now, so:
        assertTokenType(CDATA, reader.currentToken());
        str = collectAsyncText(reader, CDATA); // moves to end-element
        assertEquals("cdata\n&] ]] stuff", str);
        assertTokenType(XMLStreamConstants.END_ELEMENT, reader.currentToken());
        assertEquals("root", sr.getLocalName());
        assertEquals("", sr.getNamespaceURI());
        assertTokenType(XMLStreamConstants.PROCESSING_INSTRUCTION, reader.nextToken());
        assertEquals("pi", sr.getPITarget());
        assertEquals("data? what\ndata?", sr.getPIData());
        assertTokenType(XMLStreamConstants.END_DOCUMENT, reader.nextToken());
        assertFalse(sr.hasNext());
    }
    
    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    private int _verifyStart(AsyncReaderWrapper reader) throws Exception
    {
        // !!! TODO: should not start with START_DOCUMENT; but should get it right away
        int t = reader.nextToken();
        return t;
    }
        
    protected String collectAsyncText(AsyncReaderWrapper reader, int tt) throws XMLStreamException
    {
        StringBuilder sb = new StringBuilder();
        while (reader.currentToken() == tt) {
            sb.append(reader.currentText());
            reader.nextToken();
        }
        return sb.toString();
    }
}
