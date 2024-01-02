package com.example.wg.configuration;

import jcifs.DialectVersion;
import jcifs.smb.SmbFile;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.common.CompositeStringExpression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.FileWritingMessageHandler;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.ftp.outbound.FtpMessageHandler;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;
import org.springframework.integration.smb.outbound.SmbMessageHandler;
import org.springframework.integration.smb.session.SmbSession;
import org.springframework.integration.smb.session.SmbSessionFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import java.io.File;

@Configuration
public class genericConfiguration {

    @Bean
    public MessageChannel directMessageChannel(){
        return new DirectChannel();
    }

    @Bean
    @InboundChannelAdapter(value = "directMessageChannel", poller = @Poller(fixedDelay = "5000"))
    public FileReadingMessageSource polling(){
        FileReadingMessageSource fileReadingMessageSource = new FileReadingMessageSource();
        fileReadingMessageSource.setAutoCreateDirectory(true);
        fileReadingMessageSource.setDirectory(new File("C:\\z"));
        fileReadingMessageSource.setUseWatchService(true);
        fileReadingMessageSource.setWatchEvents(FileReadingMessageSource.WatchEventType.CREATE);
        return fileReadingMessageSource;
    }

   /* @Bean
    @ServiceActivator(inputChannel = "directMessageChannel")
    public FileWritingMessageHandler polling2(){
        FileWritingMessageHandler fileWritingMessageHandler = new FileWritingMessageHandler(new File("C:\\X"));
        fileWritingMessageHandler.setAutoCreateDirectory(true);
        return fileWritingMessageHandler;
    }
*/
    @Bean
    @ServiceActivator(inputChannel = "directMessageChannel")
    public MessageHandler sendFtp(){
        DefaultFtpSessionFactory sf = new DefaultFtpSessionFactory();
        sf.setHost("172.19.123.127");
        sf.setPort(21);
        sf.setUsername("stef");
        sf.setPassword("cerise");
        CachingSessionFactory<FTPFile> t = new CachingSessionFactory<>(sf);
        FtpMessageHandler ftpMessageHandler = new FtpMessageHandler(t);
        // setRemoteDirectoryExpressionString permet de créer le repertoire ou uploader les fichiers
        // sur le serveur ftp (vsftpd sou Ubuntu). Ici ça sera /srv/ftp
        ftpMessageHandler.setRemoteDirectoryExpressionString("headers['/srv/ftp']");
        ftpMessageHandler.setFileNameGenerator(message ->
                    message.getHeaders().get(FileHeaders.FILENAME, String.class)
        );
        ftpMessageHandler.setAutoCreateDirectory(true);
        return ftpMessageHandler;
    }

    @Bean
    @ServiceActivator(inputChannel = "directMessageChannel")
    public SmbMessageHandler sendSMB(){
        SmbSessionFactory  smbSession = new SmbSessionFactory();
        smbSession.setHost("172.19.123.127");
        smbSession.setPort(445);
        smbSession.setDomain("WORKGROUP");
        smbSession.setUsername("stef");
        smbSession.setPassword("cerise");
        smbSession.setShareAndDir("sambashare"); // nom de partage défini dans /etc/samba/smb.conf
        smbSession.setSmbMinVersion(DialectVersion.SMB202);
        smbSession.setSmbMaxVersion(DialectVersion.SMB311);

        SmbMessageHandler smbMessageHandler = new SmbMessageHandler(smbSession);
        smbMessageHandler.setAutoCreateDirectory(true);
        // remoteDirectoryExpression permet de créer un répertoire dans la path defini sur le serveur.
        // Il peut être vide ("") pour écrire à la racine du repertoire de partage.
        smbMessageHandler.setRemoteDirectoryExpression(  new LiteralExpression("test"));
        smbMessageHandler.setFileNameGenerator(message ->
                    message.getHeaders().get(FileHeaders.FILENAME, String.class)
        );
        return smbMessageHandler;
    }
}
