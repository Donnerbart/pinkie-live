package com.derpfish.pinkielive.download;

import com.derpfish.pinkielive.animation.PonyAnimation;
import com.derpfish.pinkielive.util.Base64;
import com.derpfish.pinkielive.util.IOUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import dalvik.system.DexClassLoader;

import static com.derpfish.pinkielive.util.Base64.DEFAULT;

public class PonyDownloader {

    private static final String animationsUrl = "http://animations.pinkie-live.googlecode.com/git/animations.xml";
    private static final String animationsSigUrl = "http://animations.pinkie-live.googlecode.com/git/animations.sig";

    private static final String PUBLIC_KEY_ENCODED =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA1aP3qdJO5F64+UQVF1zl" +
                    "DyyM/bN/jJZgU9guq7RLLO81y1IFpxL0Rs66FMkcSz4ZtdtOT6uRhZ9TvrVhMR69" +
                    "WpxdH3hrh4JhILd+/ZRxQt2GX4FyLDIPqfpA867ZjS+lrgncN48kC2X3z3ETQ46Q" +
                    "eCjYrLfKeseGy620dWuIV2yXenr1NHSJ5kwOKvOdddEHwijSNwpDo8C93XGCAHtT" +
                    "fXcmknBeVPNWC1iL0CshpMWuDIvLEh867J6HvpSzyAss0q62mvRyttifjZO8aiSH" +
                    "LctTLxMnTrxhL9mw4lmFCzI0UoWmeSOiEOQTXhGxWhVE9gXv0jzizTvX5DG9MtTA" +
                    "CwIDAQAB";

    private static final PublicKey PUBLIC_KEY;

    static {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.decode(PUBLIC_KEY_ENCODED, DEFAULT));
        KeyFactory keyFactory;
        PublicKey publicKey = null;
        try {
            keyFactory = KeyFactory.getInstance("RSA");
            publicKey = keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        } finally {
            PUBLIC_KEY = publicKey;
        }
    }

    public static boolean verifyData(InputStream inputStream, String base64sig) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(PUBLIC_KEY);

        byte[] buffer = new byte[4096];
        int nread;
        while ((nread = inputStream.read(buffer)) >= 0) {
            signature.update(buffer, 0, nread);
        }
        inputStream.close();

        return signature.verify(Base64.decode(base64sig, DEFAULT));
    }

    public static List<PonyAnimationListing> fetchListings() throws Exception {
        byte[] xmlBytes = fetchUrl(animationsUrl, 131072);
        byte[] sigBytes = fetchUrl(animationsSigUrl, 4096);
        if (!verifyData(new ByteArrayInputStream(xmlBytes), new String(sigBytes))) {
            throw new IllegalStateException("Unable to verify signature of animations.xml");
        }

        return parseListings(new ByteArrayInputStream(xmlBytes));
    }

    private static byte[] fetchUrl(String url, int maxSize) throws IOException {
        URLConnection connection = new URL(url).openConnection();
        connection.connect();

        InputStream inputStream = connection.getInputStream();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int totalRead = 0;
        int nRead;
        byte[] buffer = new byte[4096];
        while ((nRead = inputStream.read(buffer)) >= 0) {
            outputStream.write(buffer, 0, nRead);
            totalRead += nRead;
            if (maxSize > 0 && totalRead > maxSize) {
                throw new IllegalStateException("Retrieved animations.xml exceeds maximum allowed size.");
            }
        }
        inputStream.close();
        outputStream.close();

        return outputStream.toByteArray();
    }

    private static List<PonyAnimationListing> parseListings(InputStream inputStream) throws Exception {
        List<PonyAnimationListing> animationListings = new ArrayList<>();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document dom = builder.parse(inputStream);
        inputStream.close();

        Element root = dom.getDocumentElement();
        if (!root.getNodeName().equals("animations")) {
            throw new IllegalArgumentException("Malformed XML");
        }

        for (int i = 0; i < root.getChildNodes().getLength(); i++) {
            Node animation = root.getChildNodes().item(i);
            if (animation.getNodeType() == Node.TEXT_NODE) {
                continue;
            }

            if (!animation.getNodeName().equals("animation")) {
                throw new IllegalArgumentException("Malformed XML");
            }

            PonyAnimationListing animListing = new PonyAnimationListing();
            for (int j = 0; j < animation.getChildNodes().getLength(); j++) {
                Node attr = animation.getChildNodes().item(j);
                if (attr.getNodeType() == Node.TEXT_NODE) {
                    continue;
                }

                if (attr.getNodeName().equals("name")) {
                    if (animListing.getName() != null) {
                        throw new IllegalArgumentException("Malformed XML");
                    }
                    animListing.setName(getNodeText(attr));
                } else if (attr.getNodeName().equals("url")) {
                    if (animListing.getUrl() != null) {
                        throw new IllegalArgumentException("Malformed XML");
                    }
                    animListing.setUrl(getNodeText(attr));
                } else if (attr.getNodeName().equals("id")) {
                    if (animListing.getId() != null) {
                        throw new IllegalArgumentException("Malformed XML");
                    }
                    animListing.setId(getNodeText(attr));
                } else if (attr.getNodeName().equals("version")) {
                    if (animListing.getVersion() != null) {
                        throw new IllegalArgumentException("Malformed XML");
                    }
                    animListing.setVersion(Long.parseLong(getNodeText(attr)));
                } else if (attr.getNodeName().equals("checksum")) {
                    if (animListing.getChecksum() != null) {
                        throw new IllegalArgumentException("Malformed XML");
                    }
                    animListing.setChecksum(getNodeText(attr));
                } else {
                    throw new IllegalArgumentException("Malformed XML");
                }
            }

            if (animListing.getName() == null || animListing.getUrl() == null
                    || animListing.getId() == null || animListing.getVersion() == null
                    || animListing.getChecksum() == null) {
                throw new IllegalArgumentException("Malformed XML");
            }

            animationListings.add(animListing);
        }

        return animationListings;
    }

    private static String getNodeText(Node node) {
        if (node.getChildNodes().getLength() != 1) {
            throw new IllegalArgumentException("Malformed XML");
        }
        Node innerNode = node.getChildNodes().item(0);
        if (innerNode.getNodeType() != Node.TEXT_NODE) {
            throw new IllegalArgumentException("Malformed XML");
        }
        return innerNode.getNodeValue();
    }

    public static void fetchPony(File dataDir, File cacheDir, PonyAnimationListing animation) throws Exception {
        File ponyDir = new File(dataDir.getAbsolutePath() + File.separator + "ponies");
        if (!ponyDir.exists()) {
            ponyDir.mkdir();
        }
        File tmpFile = new File(cacheDir.getAbsolutePath() + File.separator + "delme.zip");

        URLConnection connection = new URL(animation.getUrl()).openConnection();
        connection.connect();
        IOUtils.copyStreamAndClose(connection.getInputStream(), new FileOutputStream(tmpFile));

        if (!verifyData(new FileInputStream(tmpFile), animation.getChecksum())) {
            throw new IllegalStateException("Signature verification failed.");
        }

        File animDir = new File(ponyDir.getAbsolutePath() + File.separator + animation.getId());
        if (animDir.exists()) {
            for (File file : animDir.listFiles()) {
                file.delete();
            }
        } else {
            animDir.mkdir();
        }

        InputStream inputStream = new FileInputStream(tmpFile);
        ZipInputStream zis = new ZipInputStream(inputStream);
        ZipEntry zipEntry;
        while ((zipEntry = zis.getNextEntry()) != null) {
            FileOutputStream fos = new FileOutputStream(animDir.getAbsolutePath() + File.separator + zipEntry.getName());
            IOUtils.copyStream(zis, fos);
            fos.close();
        }
        zis.close();
        inputStream.close();

        tmpFile.delete();
    }

    public static List<PonyAnimationContainer> getPonyAnimations(File dataDir, File cacheDir, boolean loadAnimations) throws Exception {
        List<PonyAnimationContainer> containers = new ArrayList<>();
        File ponyDir = new File(dataDir.getAbsolutePath() + File.separator + "ponies");
        if (!ponyDir.exists()) {
            return containers;
        }

        for (File subDir : ponyDir.listFiles()) {
            if (subDir.isDirectory()) {
                File manifest = new File(subDir.getAbsolutePath() + File.separator + "manifest.properties");
                if (!manifest.exists()) {
                    continue;
                }
                File lib = new File(subDir.getAbsolutePath() + File.separator + "lib.jar");
                if (!lib.exists()) {
                    continue;
                }

                Properties properties = new Properties();
                properties.load(new FileInputStream(manifest));

                PonyAnimationContainer container = new PonyAnimationContainer();
                container.setId(properties.getProperty("id"));
                container.setName(properties.getProperty("name"));
                container.setVersion(Long.parseLong(properties.getProperty("version")));

                if (loadAnimations) {
                    File animCacheDir = new File(cacheDir.getAbsolutePath() + File.separator + container.getId());
                    if (!animCacheDir.exists()) {
                        animCacheDir.mkdir();
                    }

                    DexClassLoader classLoader = new DexClassLoader(lib.getAbsolutePath(),
                            animCacheDir.getAbsolutePath(), null, PonyDownloader.class.getClassLoader());
                    Class<?> animClass = classLoader.loadClass(properties.getProperty("className"));
                    container.setPonyAnimation(animClass.asSubclass(PonyAnimation.class).newInstance());
                    container.getPonyAnimation().setResourceDir(subDir);
                }

                containers.add(container);
            }
        }
        return containers;
    }
}
