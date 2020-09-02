package simple.sftp;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import simple.sftp.config.SftpConfig;
import simple.sftp.exception.SimpleSftpException;

import javax.inject.Singleton;
import java.nio.charset.StandardCharsets;

/**
 * Manages SFTP sessions.
 */
@Slf4j
@Singleton
public final class SessionManager {

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
