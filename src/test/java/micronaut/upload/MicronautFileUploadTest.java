package micronaut.upload;

import static io.micronaut.http.MediaType.MULTIPART_FORM_DATA_TYPE;
import static org.assertj.core.api.Assertions.assertThat;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.multipart.MultipartBody;
import io.micronaut.runtime.EmbeddedApplication;
import io.micronaut.rxjava2.http.client.RxHttpClient;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.reactivex.Flowable;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import jakarta.inject.Inject;
import org.reactivestreams.Publisher;

@MicronautTest
class MicronautFileUploadTest {

    @Inject
    EmbeddedApplication<?> application;

    @Inject
    @Client("/")
    RxHttpClient client;

    @Test
    void testItWorks() {
        Assertions.assertTrue(application.isRunning());
    }

    @Test
    void testUploadLeak() throws IOException {
        final String filename = "micronaut_docs.pdf";
        String filePath = Objects.requireNonNull(
            getClass().getClassLoader().getResource(filename)).getPath();

        InputStream is = null;
        try {
            is = new FileInputStream(filePath);
            var requestBody = MultipartBody.builder()
                .addPart("field", "something")
                .addPart("file", filename, MediaType.APPLICATION_PDF_TYPE, is.readAllBytes())
                .build();
            var request = HttpRequest.POST("/upload", requestBody)
                .contentType(MULTIPART_FORM_DATA_TYPE);
            var i = 0;
            var requests = new ArrayList<Flowable<HttpResponse<String>>>();
            while (i < 300) {
                i++;
                requests.add(client.exchange(request, String.class));
            }

            Flowable.fromIterable(requests).blockingForEach(
                httpResponseFlowable -> assertThat(
                    httpResponseFlowable.blockingFirst().getStatus().getCode()).isEqualTo(
                    HttpStatus.OK.getCode()));
        } catch (IOException exception) {
            if (is != null) {
                is.close();
            }
        }
    }
    @Test
    void testUploadNoLeak() throws IOException {
        final String filename = "micronaut_docs.pdf";
        String filePath = Objects.requireNonNull(
            getClass().getClassLoader().getResource(filename)).getPath();

        InputStream is = null;
        try {
            is = new FileInputStream(filePath);
        var requestBody = MultipartBody.builder()
                .addPart("field", "something")
                .addPart("file", filename, MediaType.APPLICATION_PDF_TYPE, is.readAllBytes())
                .build();
            var request = HttpRequest.POST("/uploadMultipart", requestBody)
                .contentType(MULTIPART_FORM_DATA_TYPE);
            var i = 0;
            var requests = new ArrayList<Flowable<HttpResponse<String>>>();
            while (i < 300) {
                i++;
                requests.add(client.exchange(request, String.class));
            }

            Flowable.fromIterable(requests).blockingForEach(
                httpResponseFlowable -> assertThat(
                    httpResponseFlowable.blockingFirst().getStatus().getCode()).isEqualTo(
                    HttpStatus.OK.getCode()));
        } catch (IOException exception) {
            if (is != null) {
                is.close();
            }
        }
    }
}
