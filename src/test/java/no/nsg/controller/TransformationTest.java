package no.nsg.controller;

import net.sf.saxon.s9api.*;
import no.nsg.repository.TransformationManager;
import no.nsg.repository.document.formats.DocumentFormat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;


@ExtendWith(SpringExtension.class)
@Tag("UnitTest")
public class TransformationTest {
    private static Logger LOGGER = LoggerFactory.getLogger(TransformationTest.class);

    private InputStream getResourceAsStream(final String resource) {
        return getClass().getClassLoader().getResourceAsStream(resource);
    }

    @Test
    public void finvoiceHappydayTransformTest() throws SaxonApiException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TransformationManager.transform(getResourceAsStream("finvoice/Finvoice.xml"), DocumentFormat.Format.FINVOICE_PURCHASE_INVOICE, baos);
    }

    @Test
    public void finvoiceTransformTest() throws SaxonApiException {
        TransformationManager.transform(getResourceAsStream("finvoice/Finvoice.xml"), DocumentFormat.Format.FINVOICE_PURCHASE_INVOICE, new ByteArrayOutputStream());
        TransformationManager.transform(getResourceAsStream("finvoice/finvoice 75 myynti.xml"), DocumentFormat.Format.FINVOICE_PURCHASE_INVOICE, new ByteArrayOutputStream());
        TransformationManager.transform(getResourceAsStream("finvoice/finvoice 76 myynti.xml"), DocumentFormat.Format.FINVOICE_PURCHASE_INVOICE, new ByteArrayOutputStream());
        TransformationManager.transform(getResourceAsStream("finvoice/finvoice 77 myynti.xml"), DocumentFormat.Format.FINVOICE_PURCHASE_INVOICE, new ByteArrayOutputStream());
        TransformationManager.transform(getResourceAsStream("finvoice/finvoice 78 myynti.xml"), DocumentFormat.Format.FINVOICE_PURCHASE_INVOICE, new ByteArrayOutputStream());
    }

    @Test
    public void ublTransformTest() throws SaxonApiException, UnsupportedEncodingException {
        TransformationManager.transform(getResourceAsStream("ubl/Invoice_base-example.xml"), DocumentFormat.Format.UBL_2_1_PURCHASE_INVOICE, new ByteArrayOutputStream());
        TransformationManager.transform(getResourceAsStream("ubl/ehf-2-faktura-1.xml"), DocumentFormat.Format.UBL_2_1_PURCHASE_INVOICE, new ByteArrayOutputStream());
        TransformationManager.transform(getResourceAsStream("ubl/ehf-3-faktura-1.xml"), DocumentFormat.Format.UBL_2_1_PURCHASE_INVOICE, new ByteArrayOutputStream());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TransformationManager.transform(getResourceAsStream("ubl/test_purchase_invoice_for_company_id_12345.xml"), DocumentFormat.Format.UBL_2_1_PURCHASE_INVOICE, baos);
        String result = baos.toString(StandardCharsets.UTF_8.name());
        Assertions.assertTrue(result.contains("<gl-cor:identifierAuthorityCode "));

        baos = new ByteArrayOutputStream();
        TransformationManager.transform(getResourceAsStream("ubl/test_purchase_invoice_for_company_id_12345.xml"), DocumentFormat.Format.UBL_2_1_SALES_INVOICE, baos);
        result = baos.toString(StandardCharsets.UTF_8.name());
        Assertions.assertTrue(result.contains("<gl-cor:identifierAuthorityCode "));
    }
}
