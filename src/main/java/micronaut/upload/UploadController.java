package micronaut.upload;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

import io.micronaut.context.annotation.Parameter;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.http.multipart.CompletedPart;
import io.micronaut.http.server.multipart.MultipartBody;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableSubscriber;
import io.reactivex.Single;
import io.reactivex.annotations.NonNull;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.ReplaySubject;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Phaser;
import javax.validation.ConstraintViolationException;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExecuteOn(TaskExecutors.IO)
@Controller
public class UploadController {
  private static final Logger LOGGER = LoggerFactory.getLogger(UploadController.class);

  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Post(
      value = "/upload",
      consumes = MediaType.MULTIPART_FORM_DATA,
      produces = MediaType.TEXT_PLAIN)
  public Publisher<HttpResponse> upload(
      Flowable<CompletedFileUpload> file,
      String field) {

    ReplaySubject<HttpResponse> subject = ReplaySubject.create();

    file.subscribeOn(Schedulers.io())
        .subscribe(
            new FlowableSubscriber<CompletedFileUpload>() {
              Subscription subscription;

              @Override
              public void onSubscribe(@NonNull Subscription s) {
                s.request(1);
                this.subscription = s;
              }

              void exit(Throwable e) {
                subscription.cancel();
                subject.onError(e);
              }

              @Override
              public void onNext(CompletedFileUpload upload) {
                final String fileName = upload.getFilename();
                // Always read the data to make sure it's released
                byte[] dataToSend = null;
                try {
                  dataToSend = upload.getBytes();
                } catch (IOException exception) {
                  exit(new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not read file"));
                }

                LOGGER.info("File {}, size {} for field {} read successfully", upload.getFilename(), upload.getSize(), field);

                subscription.request(1);
              }

              @Override
              public void onError(Throwable t) {
                subject.onError(t);
              }

              @Override
              public void onComplete() {
                subject.onNext(HttpResponse.ok(HttpResponse.ok()));
                subject.onComplete();
              }
            });

    return subject.toFlowable(BackpressureStrategy.ERROR);
  }

  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Post(
      value = "/uploadMultipart",
      consumes = MediaType.MULTIPART_FORM_DATA,
      produces = MediaType.TEXT_PLAIN)
  public Single<HttpResponse> uploadMultipart(
      @Body MultipartBody body) {
    return Single.create(emitter ->
        body.subscribe(
            new FlowableSubscriber<CompletedPart>() {
              Subscription subscription;

              @Override
              public void onSubscribe(@NonNull Subscription s) {
                s.request(1);
                this.subscription = s;
              }

              void exit(Throwable e) {
                subscription.cancel();
              }

              @Override
              public void onNext(CompletedPart upload) {
                // Always read the data to make sure it's released
                byte[] dataToSend;
                try {
                  dataToSend = upload.getBytes();
                  LOGGER.info("Part {}, size {} read successfully", upload.getName(), dataToSend.length);
                } catch (IOException exception) {
                  exit(new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not read file"));
                }
                subscription.request(1);
              }

              @Override
              public void onError(Throwable t) {
                emitter.onError(t);
              }

              @Override
              public void onComplete() {
                emitter.onSuccess(HttpResponse.ok(HttpResponse.ok()));
              }
            }));
  }
}
