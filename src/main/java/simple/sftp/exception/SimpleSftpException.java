package simple.sftp.exception;

public class SimpleSftpException extends Exception {

    public SimpleSftpException(Throwable cause) {
        super(cause);
    }

    public SimpleSftpException(String message) {
        super(message);
    }
}
