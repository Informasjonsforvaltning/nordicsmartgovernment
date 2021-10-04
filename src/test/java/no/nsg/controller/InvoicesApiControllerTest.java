package no.nsg.controller;

import no.nsg.repository.ConnectionManager;
import no.nsg.repository.MimeType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


@SpringBootTest
@ExtendWith(SpringExtension.class)
@ContextConfiguration(initializers = {InvoicesApiControllerTest.Initializer.class})
@Tag("ServiceTest")
@Testcontainers
public class InvoicesApiControllerTest {

    @Mock
    HttpServletRequest httpServletRequestMock;

    @Mock
    HttpServletResponse httpServletResponseMock;

    @Autowired
    DocumentApi invoicesApiController;

    @Autowired
    TransactionsApi transactionsApiController;

    @Autowired
    ConnectionManager connectionManager;

    @Container
    public static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:latest")
            .withDatabaseName("integration-tests-db")
            .withUsername("testuser")
            .withPassword("testpassword");

    static class Initializer
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues.of(
                    "spring.datasource.url=" + postgreSQLContainer.getJdbcUrl(),
                    "spring.datasource.username=" + postgreSQLContainer.getUsername(),
                    "spring.datasource.password=" + postgreSQLContainer.getPassword(),
                    "postgres.nsg.db_url=" + postgreSQLContainer.getJdbcUrl(),
                    "postgres.nsg.dbo_user=" + postgreSQLContainer.getUsername(),
                    "postgres.nsg.dbo_password=" + postgreSQLContainer.getPassword(),
                    "postgres.nsg.user=" + postgreSQLContainer.getUsername(),
                    "postgres.nsg.password=" + postgreSQLContainer.getPassword()
            ).applyTo(configurableApplicationContext.getEnvironment());
        }
    }

    @BeforeEach
    public void before() throws IOException {
        connectionManager.waitUntilSyntheticDataIsImported();
    }

    @Test
    public void happyDay()
    {
        Assertions.assertTrue(true);
    }

    @Test
    public void createFinvoiceTest() throws IOException, NoSuchAlgorithmException {
        final String companyId = "2372513-5";
        Mockito.when(httpServletRequestMock.getContentType()).thenReturn(MimeType.NSG_SALES_INVOICE);

        String original = resourceAsString("finvoice/Finvoice.xml", StandardCharsets.UTF_8);
        String originalChecksum = sha256Checksum(original.getBytes(StandardCharsets.UTF_8));

        ResponseEntity<Void> createResponse = invoicesApiController.createDocument(httpServletRequestMock, httpServletResponseMock, companyId, original);
        Assertions.assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        URI location = createResponse.getHeaders().getLocation();
        String[] paths = location.getPath().split("/");
        String createdTransactionId = paths[paths.length-2];
        String createdDocumentId = paths[paths.length-1];

        ResponseEntity<Object> response2 = invoicesApiController.getDocumentById(httpServletRequestMock, httpServletResponseMock, companyId, createdTransactionId, createdDocumentId);
        Assertions.assertSame(response2.getStatusCode(), HttpStatus.OK);
        DocumentApi.Document returnedInvoice = (DocumentApi.Document) response2.getBody();
        String returnedInvoiceChecksum = sha256Checksum(returnedInvoice.original);
        Assertions.assertEquals(originalChecksum, returnedInvoiceChecksum);
    }

    @Test
    public void createInvoiceTest() throws IOException {
        final String companyId = "983294";
        Mockito.when(httpServletRequestMock.getContentType()).thenReturn(MimeType.NSG_SALES_INVOICE);
        ResponseEntity<Void> response = invoicesApiController.createDocument(httpServletRequestMock, httpServletResponseMock, companyId, resourceAsString("ubl/Invoice_base-example.xml", StandardCharsets.UTF_8));
        Assertions.assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    public void createPurchaseInvoiceTest() throws IOException {
        final String companyId = "003705140395";
        Mockito.when(httpServletRequestMock.getContentType()).thenReturn(MimeType.NSG_PURCHASE_INVOICE);
        ResponseEntity<Void> response = invoicesApiController.createDocument(httpServletRequestMock, httpServletResponseMock, companyId, resourceAsString("finvoice/finvoice 78 myynti.xml", StandardCharsets.UTF_8));
        Assertions.assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    public void createInvoiceSellerBuyerTest() throws IOException {
        final String sellerCompanyId = "2431081-2";
        final String buyerCompanyId = "1199940-3";
        Mockito.when(httpServletRequestMock.getContentType()).thenReturn(MimeType.NSG_SALES_INVOICE);
        ResponseEntity<Void> response = invoicesApiController.createDocument(httpServletRequestMock, httpServletResponseMock, sellerCompanyId, resourceAsString("finvoice/finvoice_eKuitti.xml", StandardCharsets.UTF_8));
        Assertions.assertEquals(HttpStatus.CREATED, response.getStatusCode());

        response = invoicesApiController.createDocument(httpServletRequestMock, httpServletResponseMock, buyerCompanyId, resourceAsString("finvoice/finvoice_eKuitti.xml", StandardCharsets.UTF_8));
        Assertions.assertEquals(HttpStatus.NOT_ACCEPTABLE, response.getStatusCode());

        Mockito.when(httpServletRequestMock.getContentType()).thenReturn(MimeType.NSG_PURCHASE_INVOICE);
        response = invoicesApiController.createDocument(httpServletRequestMock, httpServletResponseMock, sellerCompanyId, resourceAsString("finvoice/finvoice_eKuitti.xml", StandardCharsets.UTF_8));
        Assertions.assertEquals(HttpStatus.NOT_ACCEPTABLE, response.getStatusCode());

        response = invoicesApiController.createDocument(httpServletRequestMock, httpServletResponseMock, buyerCompanyId, resourceAsString("finvoice/finvoice_eKuitti.xml", StandardCharsets.UTF_8));
        Assertions.assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    public void createPurchaseInvoiceTestTest() throws IOException {
        final String companyId = "20202020";
        Mockito.when(httpServletRequestMock.getContentType()).thenReturn(MimeType.NSG_PURCHASE_INVOICE);
        ResponseEntity<Void> response = invoicesApiController.createDocument(httpServletRequestMock, httpServletResponseMock, companyId, resourceAsString("ubl/test_purchase_invoice.xml", StandardCharsets.UTF_8));
        Assertions.assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    public void createEUPurchaseInvoiceTest() throws IOException {
        final String companyId = "003711999403";
        Mockito.when(httpServletRequestMock.getContentType()).thenReturn(MimeType.NSG_PURCHASE_INVOICE);
        ResponseEntity<Void> response = invoicesApiController.createDocument(httpServletRequestMock, httpServletResponseMock, companyId, resourceAsString("finvoice/finvoiceTestPurchaseEU.xml", StandardCharsets.UTF_8));
        Assertions.assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    public void createEUSalesInvoiceTest() throws IOException {
        final String companyId = "2372513-5";
        Mockito.when(httpServletRequestMock.getContentType()).thenReturn(MimeType.NSG_SALES_INVOICE);
        ResponseEntity<Void> response = invoicesApiController.createDocument(httpServletRequestMock, httpServletResponseMock, companyId, resourceAsString("finvoice/finvoiceTestPurchaseEU.xml", StandardCharsets.UTF_8));
        Assertions.assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    public void createEUSalesInvoiceWithAttachmentTest() throws IOException {
        final String companyId = "2372513-5";
        Mockito.when(httpServletRequestMock.getContentType()).thenReturn(MimeType.NSG_SALES_INVOICE);
        ResponseEntity<Void> createResponse = invoicesApiController.createDocument(httpServletRequestMock, httpServletResponseMock, companyId, resourceAsString("finvoice/finvoiceTestPurchaseEU.xml", StandardCharsets.UTF_8));
        Assertions.assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());

        URI location = createResponse.getHeaders().getLocation();
        String[] paths = location.getPath().split("/");
        String createdTransactionId = paths[paths.length-2];
        Mockito.when(httpServletRequestMock.getContentType()).thenReturn(MimeType.NSG_OTHER);
        ResponseEntity<Void> createResponse2 = invoicesApiController.createDocumentInTransaction(httpServletRequestMock, httpServletResponseMock, companyId, createdTransactionId, "asdf");
        Assertions.assertEquals(HttpStatus.CREATED, createResponse2.getStatusCode());
        Assertions.assertEquals(createdTransactionId, createResponse2.getHeaders().getLocation().getPath().split("/")[3]);

        Mockito.when(httpServletRequestMock.getHeader("Accept")).thenReturn(MimeType.XBRL_GL);
        ResponseEntity<Object> transactionResponse = transactionsApiController.getTransactionById(httpServletRequestMock, httpServletResponseMock, companyId, createdTransactionId);
        Assertions.assertSame(transactionResponse.getStatusCode(), HttpStatus.OK);
    }

    @Test
    public void getInvoicesTest() {
        final String companyId = "todo";
        Mockito.when(httpServletRequestMock.getHeader("Accept")).thenReturn(MimeType.JSON);
        ResponseEntity<Object> response = invoicesApiController.getDocuments(httpServletRequestMock, httpServletResponseMock, companyId, MimeType.NSG_SALES_INVOICE);
        Assertions.assertTrue(response.getStatusCode()==HttpStatus.OK || response.getStatusCode()==HttpStatus.NO_CONTENT);
    }

    @Test
    public void getInvoiceByIdTest() throws IOException {
        final String companyId = "123456785";
        Mockito.when(httpServletRequestMock.getContentType()).thenReturn(MimeType.NSG_SALES_INVOICE);
        ResponseEntity<Void> createResponse = invoicesApiController.createDocument(httpServletRequestMock, httpServletResponseMock, companyId, resourceAsString("ubl/ehf-2-faktura-1.xml", StandardCharsets.UTF_8));
        Assertions.assertSame(createResponse.getStatusCode(), HttpStatus.CREATED);
        URI location = createResponse.getHeaders().getLocation();
        String[] paths = location.getPath().split("/");
        String createdTransactionId = paths[paths.length-2];
        String createdDocumentId = paths[paths.length-1];

        ResponseEntity<Object> response = invoicesApiController.getDocumentById(httpServletRequestMock, httpServletResponseMock, companyId, createdTransactionId, createdDocumentId);
        Assertions.assertSame(response.getStatusCode(), HttpStatus.OK);

        DocumentApi.Document invoice = (DocumentApi.Document) response.getBody();
        Assertions.assertEquals(createdDocumentId, invoice.documentid);
    }

    private static String resourceAsString(final String resource, final Charset charset) throws IOException {
        InputStream resourceStream = InvoicesApiControllerTest.class.getClassLoader().getResourceAsStream(resource);

        StringBuilder sb = new StringBuilder();
        String line;
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(resourceStream, charset))) {
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
        } catch (NullPointerException e) {
            throw e;
        }
        return sb.toString();
    }

    private String sha256Checksum(final byte[] content) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(content);

        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
