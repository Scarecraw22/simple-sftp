package simple.sftp;

import simple.sftp.exception.DownloadSftpException;
import simple.sftp.exception.UploadSftpException;

import java.io.File;
import java.io.InputStream;
import java.util.function.Consumer;

/**
 * Manages files on remote server. Gives possibility to
 * upload and download files from remote server.
 */
public interface SftpService {

    void upload(InputStream inputStream, String destinationPath) throws UploadSftpException;

    void upload(File sourceFile, String destinationPath) throws UploadSftpException;

    void download(String sourcePath, File destinationFile) throws DownloadSftpException;

    void onDownload(String sourcePath, Consumer<InputStream> onDownloadConsumer) throws DownloadSftpException;

}
