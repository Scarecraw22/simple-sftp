package simple.sftp.config;

import com.google.inject.BindingAnnotation;
import com.typesafe.config.Config;
import simple.sftp.SftpService;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Defines configuration for {@link SftpService}.
 */
public interface SftpConfig {

    /**
     * Annotation that helps binding {@link Config} in Guice.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @BindingAnnotation
    @interface Annotation {}

    /**
     * Gets host address from {@link Config}
     *
     * @return Host address used in {@link SftpService} configuration.
     */
    String getHost();

    /**
     * Gets port number from {@link Config}.
     *
     * @return Port number used in {@link SftpService} configuration.
     */
    Integer getPort();

    /**
     * Gets username from {@link Config}.
     *
     * @return Username used in {@link SftpService} configuration.
     */
    String getUsername();

    /**
     * Gets password from {@link Config}.
     *
     * @return Password used in {@link SftpService} configuration.
     */
    String getPassword();
}
