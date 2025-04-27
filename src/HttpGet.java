import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.List;

public class HttpGet {
    private static final List<String> certs = List.of("root.cer");
    private static final Path trustStorePath = Path.of(System.getProperty("java.io.tmpdir"), "test.keystore");
    private static final char[] password = "123456".toCharArray();
    public static final String JKS = "JKS";

    public static void main(String[] args) throws Exception {

        KeyStore keyStore = KeyStore.getInstance(JKS);
        keyStore.load(null, null);
        for (String certFile : certs) {
            try (InputStream fileInputStream = Files.newInputStream(Path.of(certFile));
                 BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream)
            ) {
                while (bufferedInputStream.available() > 0) {
                    Certificate certificate = CertificateFactory.getInstance("X.509").generateCertificate(bufferedInputStream);
                    keyStore.setCertificateEntry(certFile, certificate);
                }
            }
        }

        try(OutputStream outputStream = Files.newOutputStream(trustStorePath)) {
        	keyStore.store(outputStream, password);
        }

        System.setProperty("javax.net.ssl.trustStore", trustStorePath.toString());
        System.setProperty("javax.net.ssl.trustStorePassword", new String(password));
        System.setProperty("javax.net.ssl.trustStoreType", JKS);


        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("https://untrusted-root.badssl.com/"))
                .build();
        HttpResponse<String> response = HttpClient
                .newBuilder()
                .build()
                .send(request, HttpResponse.BodyHandlers.ofString());

        response.headers().map().forEach((k, v) -> System.out.println(k + "=" + v));

        System.out.println(response.statusCode());
        System.out.println(response.body());

        Thread.sleep(10000);

        trustStorePath.toFile().deleteOnExit();
    }
}
