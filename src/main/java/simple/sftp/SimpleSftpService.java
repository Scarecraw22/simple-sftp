package simple.sftp;

import com.jcraft.jsch.*;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import simple.sftp.config.SftpConfig;
import simple.sftp.exception.DownloadSftpException;
import simple.sftp.exception.SimpleSftpException;
import simple.sftp.exception.UploadSftpException;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.function.Consumer;

/**
 * Implementation of {@link SftpService}. Uses {@link SessionManager} to
 * create {@link Session}. Uses {@link ChannelSftp} for uploading and downloading files.
 */
@Slf4j
@Singleton
public class SimpleSftpService implements SftpService {

    private final SessionManager sessionManager;
    private final SftpConfig config;

    @Inject
    public SimpleSftpService(SftpConfig sftpConfig) {
        this.sessionManager = new SessionManager();
        this.config = sftpConfig;
    }

    /**
     * Uploads {@link InputStream} to remote server. File on remote server
     * will be uploaded on given destination.
     *
     * @param inputStream     {@link InputStream} to upload.
     * @param destinationPath Path that {@link InputStream} will be saved on.
     * @throws UploadSftpException Thrown when uploading {@link InputStream} fails.
     */
    @Override
    public void upload(InputStream inputStream, String destinationPath) throws UploadSftpException {
        tryOrThrowUploadException(channelSftp -> {
            tryUploadOrThrowUploadException(channelSftp, inputStream, destinationPath);
        });
    }

    /**
     * Uploads {@link File} to remote server. {@link File} on remote server
     * will be uploaded on given destination.
     *
     * @param sourceFile      {@link File} to upload.
     * @param destinationPath Path that {@link File} will be saved on.
     * @throws UploadSftpException Thrown when uploading {@link File} fails.
     */
    @Override
    public void upload(File sourceFile, String destinationPath) throws UploadSftpException {
        tryOrThrowUploadException(channelSftp -> {
            try (InputStream inputStream = Files.newInputStream(sourceFile.toPath())) {
                tryUploadOrThrowUploadException(channelSftp, inputStream, destinationPath);

            } catch (IOException e) {
                log.warn("Error while creating InputStream from file: {}", sourceFile.getAbsolutePath());
                throw new UploadSftpException(e);
            }
        });
    }

    /**
     * Downloads file on given path from remote server and saves it to given {@link File}.
     *
     * @param sourcePath      Path that desired file exists on remote server.
     * @param destinationFile {@link File} that will be copy of file from remote server.
     * @throws DownloadSftpException Thrown when downloading fails.
     */
    @Override
    public void download(String sourcePath, File destinationFile) throws DownloadSftpException {
        tryOrThrowDownloadException(channelSftp -> {
            try {
                FileUtils.copyInputStreamToFile(tryDownloadOrThrowDownloadException(channelSftp, sourcePath), destinationFile);

            } catch (IOException e) {
                log.warn("Error while trying to copy InputStream to file: {}", destinationFile.getAbsolutePath());
                throw new DownloadSftpException(e);
            }
        });
    }

    /**
     * Downloads file on given path from remote server and runs given {@link Consumer} on
     * downloaded {@link InputStream}.
     *
     * @param sourcePath         Path that desired file exists on remote server.
     * @param onDownloadConsumer Consumer that will accept {@link InputStream} after it's downloaded.
     * @throws DownloadSftpException Thrown when downloading fails.
     */
    @Override
    public void onDownload(String sourcePath, Consumer<InputStream> onDownloadConsumer) throws DownloadSftpException {
        tryOrThrowDownloadException(channelSftp -> {
            try (InputStream inputStream = tryDownloadOrThrowDownloadException(channelSftp, sourcePath)) {
                onDownloadConsumer.accept(inputStream);

            } catch (IOException e) {
                log.warn("Error while closing InputStream");
                throw new DownloadSftpException(e);
            }
        });
    }

    private InputStream tryDownloadOrThrowDownloadException(ChannelSftp channelSftp,
                                                            String sourcePath) throws DownloadSftpException {
        try {
            log.debug("Trying to download file: {}", sourcePath);
            return channelSftp.get(sourcePath);
        } catch (SftpException e) {
            log.warn("Error while downloading file from: {}", sourcePath);
            throw new DownloadSftpException(e);
        }
    }

    private void tryUploadOrThrowUploadException(ChannelSftp channelSftp,
                                                 InputStream inputStream,
                                                 String destinationPath) throws UploadSftpException {
        try {
            log.debug("Trying to upload file to: {}", destinationPath);
            channelSftp.put(inputStream, destinationPath);
        } catch (SftpException e) {
            log.warn("Error while uploading file to: {}", destinationPath);
            throw new UploadSftpException(e);
        }
    }

    private void tryOrThrowUploadException(ChannelSftpConsumer consumer) throws UploadSftpException {
        try {
            doOnConnected(consumer);
        } catch (SimpleSftpException e) {
            throw new UploadSftpException(e);
        }
    }

    private void tryOrThrowDownloadException(ChannelSftpConsumer consumer) throws DownloadSftpException {
        try {
            doOnConnected(consumer);
        } catch (SimpleSftpException e) {
            throw new DownloadSftpException(e);
        }
    }

    private void doOnConnected(ChannelSftpConsumer consumer) throws SimpleSftpException {
        String host = config.getHost();
        Integer port = config.getPort();

        Session session = null;
        ChannelSftp channelSftp = null;

        log.debug("Trying to connect to: {}:{}", host, port);
        try {
            session = sessionManager.getSession(config);
            session.connect();
            log.debug("Connected !");

            log.debug("Trying to open SFTP channel");
            channelSftp = (ChannelSftp) session.openChannel("sftp");
            log.debug("Opened !");

            consumer.accept(channelSftp);
        } catch (JSchException | SimpleSftpException e) {
            log.warn("Error while connecting to: {}:{}", host, port);
            throw new SimpleSftpException(e);
        } finally {
            if (channelSftp != null) {
                log.debug("Trying to close SFTP channel");
                channelSftp.disconnect();
            }
            if (session != null) {
                log.debug("Trying to close session");
                session.disconnect();
            }
        }
    }

    private interface ChannelSftpConsumer {
        void accept(ChannelSftp channelSftp) throws SimpleSftpException;
    }

    private static final class SessionManager {
        private final JSch jsch;

        public SessionManager() {
            this.jsch = new JSch();
        }

        /**
         * Creates {@link Session} from {@link SftpConfig}.
         *
         * @param config {@link SftpConfig} required to create and configure {@link Session}.
         * @return {@link Session} used in {@link SimpleSftpService}.
         * @throws SimpleSftpException Thrown when creating and configuring {@link Session} fails.
         */
        public Session getSession(@NonNull SftpConfig config) throws SimpleSftpException {
            try {
                Session session = jsch.getSession(config.getUsername(), config.getHost(), config.getPort());
                session.setPassword(config.getPassword().getBytes(StandardCharsets.UTF_8));
                return session;
            } catch (JSchException e) {
                log.warn("Error while creating SFTP session");
                throw new SimpleSftpException(e);
            }
        }
    }
}
