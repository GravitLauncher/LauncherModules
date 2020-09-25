package pro.gravit.launchermodules.generatecertificate;

import org.bouncycastle.asn1.DERBMPString;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.crypto.engines.DESedeEngine;
import org.bouncycastle.crypto.engines.RC2Engine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS12PfxPdu;
import org.bouncycastle.pkcs.PKCS12PfxPduBuilder;
import org.bouncycastle.pkcs.PKCS12SafeBag;
import org.bouncycastle.pkcs.PKCS12SafeBagBuilder;
import org.bouncycastle.pkcs.bc.BcPKCS12MacCalculatorBuilder;
import org.bouncycastle.pkcs.bc.BcPKCS12PBEOutputEncryptorBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS12SafeBagBuilder;
import pro.gravit.launcher.Launcher;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.launchserver.config.LaunchServerConfig;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class GenerateCertificateCommand extends Command {
    public GenerateCertificateCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[]";
    }

    @Override
    public String getUsageDescription() {
        return "Generate self-signed certificate";
    }

    @Override
    public void invoke(String... args) throws Exception {
        String projectName = server.config.projectName;
        LogHelper.info("Generate certificates for project %s", projectName);
        LogHelper.info("Generate RSA key pair for Root CA");
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048, SecurityHelper.newRandom());
        KeyPair pair = generator.generateKeyPair();
        LogHelper.info("Generate CA Certificate");
        SecureRandom random = SecurityHelper.newRandom();
        LocalDateTime startDate = LocalDate.now().atStartOfDay();

        X500NameBuilder subject = new X500NameBuilder();
        subject.addRDN(BCStyle.CN, projectName.concat(" Root CA"));
        subject.addRDN(BCStyle.O, projectName);

        X509v3CertificateBuilder builder = new X509v3CertificateBuilder(
                subject.build(),
                BigInteger.valueOf(random.nextLong()),
                Date.from(startDate.atZone(ZoneId.systemDefault()).toInstant()),
                Date.from(startDate.plusDays(3650).atZone(ZoneId.systemDefault()).toInstant()),
                subject.build(),
                SubjectPublicKeyInfo.getInstance(pair.getPublic().getEncoded()));
        builder.addExtension(Extension.basicConstraints, false, new BasicConstraints(0));
        JcaContentSignerBuilder csBuilder = new JcaContentSignerBuilder("SHA256WITHRSA");
        ContentSigner signer = csBuilder.build(pair.getPrivate());
        X509CertificateHolder caCertificate = builder.build(signer);

        LogHelper.info("Generate RSA key pair for ending certificate");
        KeyPair endingPair = generator.generateKeyPair();
        LogHelper.info("Generate ending certificate");
        X500NameBuilder endingSubject = new X500NameBuilder();
        endingSubject.addRDN(BCStyle.CN, projectName.concat(" Code Sign"));
        endingSubject.addRDN(BCStyle.O, projectName);

        X509v3CertificateBuilder endingBuilder = new X509v3CertificateBuilder(
                caCertificate.getSubject(),
                BigInteger.valueOf(random.nextLong()),
                Date.from(startDate.atZone(ZoneId.systemDefault()).toInstant()),
                Date.from(startDate.plusDays(365).atZone(ZoneId.systemDefault()).toInstant()),
                endingSubject.build(),
                SubjectPublicKeyInfo.getInstance(endingPair.getPublic().getEncoded()));
        ExtendedKeyUsage extendedKeyUsage = new ExtendedKeyUsage(new KeyPurposeId[]{KeyPurposeId.id_kp_codeSigning});
        endingBuilder.addExtension(Extension.extendedKeyUsage, false, extendedKeyUsage);
        endingBuilder.addExtension(Extension.keyUsage, false, new KeyUsage(KeyUsage.digitalSignature));
        JcaContentSignerBuilder endingCsBuilder = new JcaContentSignerBuilder("SHA256WITHRSA");
        ContentSigner endingSigner = endingCsBuilder.build(pair.getPrivate());
        X509CertificateHolder endingCertificate = endingBuilder.build(endingSigner);
        LogHelper.info("Save certificates to disk");
        server.certificateManager.writeCertificate(server.dir.resolve(projectName.concat("RootCA.crt")), caCertificate);
        server.certificateManager.writePrivateKey(server.dir.resolve(projectName.concat("RootCA.key")), pair.getPrivate());

        server.certificateManager.writeCertificate(server.dir.resolve(projectName.concat("CodeSign.crt")), caCertificate);
        server.certificateManager.writePrivateKey(server.dir.resolve(projectName.concat("CodeSign.key")), pair.getPrivate());

        LogHelper.info("Prepare PKCS#12 keystore");
        JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();
        PKCS12PfxPduBuilder pkcsBuilder = new PKCS12PfxPduBuilder();
        PKCS12SafeBagBuilder caCertBagBuilder = new PKCS12SafeBagBuilder(caCertificate);
        caCertBagBuilder.addBagAttribute(PKCSObjectIdentifiers.pkcs_9_at_friendlyName, new DERBMPString(projectName.concat("RootCA")));
        PKCS12SafeBagBuilder endingCertBagBuilder = new PKCS12SafeBagBuilder(endingCertificate);
        String passwd = SecurityHelper.randomStringToken();
        endingCertBagBuilder.addBagAttribute(PKCSObjectIdentifiers.pkcs_9_at_friendlyName, new DERBMPString(projectName.concat("CodeSign")));
        endingCertBagBuilder.addBagAttribute(PKCSObjectIdentifiers.pkcs_9_at_localKeyId, extUtils.createSubjectKeyIdentifier(endingPair.getPublic()));
        PKCS12SafeBagBuilder keyBagBuilder = new JcaPKCS12SafeBagBuilder(endingPair.getPrivate(), new BcPKCS12PBEOutputEncryptorBuilder(PKCSObjectIdentifiers.pbeWithSHAAnd3_KeyTripleDES_CBC, new CBCBlockCipher(new DESedeEngine())).build(passwd.toCharArray()));
        keyBagBuilder.addBagAttribute(PKCSObjectIdentifiers.pkcs_9_at_friendlyName, new DERBMPString(projectName.concat("CodeSign")));
        keyBagBuilder.addBagAttribute(PKCSObjectIdentifiers.pkcs_9_at_localKeyId, extUtils.createSubjectKeyIdentifier(endingPair.getPublic()));
        PKCS12SafeBag[] certs = new PKCS12SafeBag[2];
        certs[1] = caCertBagBuilder.build();
        certs[0] = endingCertBagBuilder.build();
        pkcsBuilder.addData(keyBagBuilder.build());
        pkcsBuilder.addEncryptedData(new BcPKCS12PBEOutputEncryptorBuilder(PKCSObjectIdentifiers.pbeWithSHAAnd40BitRC2_CBC, new CBCBlockCipher(new RC2Engine())).build(passwd.toCharArray()), certs);
        PKCS12PfxPdu pfx = pkcsBuilder.build(new BcPKCS12MacCalculatorBuilder(), passwd.toCharArray());
        LogHelper.info("Save PKCS#12 keystore");
        try (OutputStream output = IOHelper.newOutput(server.dir.resolve(projectName.concat("CodeSign.p12")))) {
            output.write(pfx.getEncoded());
        }
        LogHelper.info("Generate sign config");
        LaunchServerConfig.JarSignerConf conf = new LaunchServerConfig.JarSignerConf();
        conf.enabled = true;
        conf.keyPass = passwd;
        conf.keyStorePass = passwd;
        conf.keyStoreType = "PKCS12";
        conf.signAlgo = "SHA256WITHRSA";
        conf.keyAlias = projectName.concat("CodeSign").toLowerCase();
        conf.keyStore = server.dir.resolve(projectName.concat("CodeSign.p12")).toString();
        LogHelper.info("Configuration: %s", Launcher.gsonManager.configGson.toJson(conf));
        LogHelper.info("KeyAlias may be incorrect. Usage: 'keytool -storepass %s -keystore %s -list' for check alias", passwd, conf.keyStore);
        LogHelper.warning("Must save your store password");
        if (!server.config.sign.enabled) {
            LogHelper.info("Write temporary sign config(in memory, reset on restart)");
            server.config.sign = conf;
            LogHelper.info("Add your RootCA to truststore");
            Path pathToRootCA = server.dir.resolve("truststore").resolve(projectName.concat("RootCA.crt"));
            Files.deleteIfExists(pathToRootCA);
            Files.copy(server.dir.resolve(projectName.concat("RootCA.crt")), pathToRootCA);
            server.certificateManager.readTrustStore(server.dir.resolve("truststore"));
        } else {
            Path pathToRootCA = server.dir.resolve("truststore").resolve(projectName.concat("RootCA.crt"));
            Files.deleteIfExists(pathToRootCA);
            Files.copy(server.dir.resolve(projectName.concat("RootCA.crt")), pathToRootCA);
        }
        //caKey = PrivateKeyFactory.createKey(pair.getPrivate().getEncoded());
    }
}
