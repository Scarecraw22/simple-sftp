package simple.sftp

import com.google.common.io.Resources
import org.testcontainers.containers.GenericContainer
import org.testcontainers.images.builder.ImageFromDockerfile
import simple.sftp.config.SftpConfig
import spock.lang.Specification

import java.nio.file.Path

class SimpleSftpIT extends Specification {

    private GenericContainer container = new GenericContainer(
            new ImageFromDockerfile("sftp-service", true)
            .withDockerfile(Path.of(Resources.getResource("sftp/Dockerfile").toURI()))
    )

    private SftpConfig sftpConfig = Mock()
    private SftpService sftpService

    def setup() {
        container.start()
        sftpConfig.getHost() >> container.getHost()
        sftpConfig.getPort() >> container.getMappedPort(22)
        sftpConfig.getUsername() >> "sample-username"
        sftpConfig.getPassword() >> "sample-password"

        sftpService = new SimpleSftpService(sftpConfig)
    }

    def 'test'() {
        expect:
        1 == 1
    }
}
