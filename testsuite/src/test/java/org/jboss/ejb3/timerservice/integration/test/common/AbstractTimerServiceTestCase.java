/*
* JBoss, Home of Professional Open Source
* Copyright 2005, JBoss Inc., and individual contributors as indicated
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.jboss.ejb3.timerservice.integration.test.common;


import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archives;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.importer.ExplodedImporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * AbstractTimerServiceTestCase
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public abstract class AbstractTimerServiceTestCase {

    private final static String DEPLOYER_NAME = "jboss.system:service=MainDeployer";

    protected MBeanServerConnection server;

    private Context ctx;

    /**
     * Logger
     */
    private static Logger logger = Logger.getLogger(AbstractTimerServiceTestCase.class);

    /**
     * basedir (set through Maven)
     */
    protected static final File BASEDIR = new File(System.getProperty("basedir"));

    /**
     * Target directory
     */
    protected static final File TARGET_DIRECTORY = new File(BASEDIR, "target");

    /**
     * The directory into which the deployments required by the tests will be placed
     */
    protected static final File TEST_DEPLOYMENTS_FOLDER = new File(TARGET_DIRECTORY, "test-lib");

    /**
     * The <project>/src/test/resources folder
     */
    protected static final File TEST_SRC_RESOURCES = new File(BASEDIR, "src/test/resources");

    static {
        if (!TEST_DEPLOYMENTS_FOLDER.exists()) {
            TEST_DEPLOYMENTS_FOLDER.mkdir();
        }
    }

    /**
     * @param name
     */
    public AbstractTimerServiceTestCase() {
        try {
            this.ctx = new InitialContext();
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the URL to the {@code resourceName}. The resource is looked
     * under {@code klass}'s package name. For example, if the
     * klass is org.jboss.ejb3.nointerface.integration.test.simple.unit.BasicTestCase and the
     * resource name is ejb-jar.xml, then this method looks for ejb-jar.xml under
     * org/jboss/ejb3/nointerface/integration/test/simple/unit in the classpath.
     * <p/>
     * This method returns null if the resource was not found in the classpath
     *
     * @param klass        The Class under whose package the resources are searched for
     * @param resourceName The name of the resource
     * @return Returns the URL to the resource or NULL if not found in classpath
     */
    protected static URL getResource(Class<?> klass, String resourceName) {
        String resourceDir = klass.getPackage().getName().toString() + ".";
        resourceDir = resourceDir.replace('.', '/');
        String resourcePath = resourceDir + resourceName;
        return klass.getClassLoader().getResource(resourcePath);
    }

    /**
     * Returns the URLs to the {@code resources}. The resources are looked
     * under {@code klass}'s package name. For example, if the
     * klass is org.jboss.ejb3.nointerface.integration.test.simple.unit.BasicTestCase and the
     * resource name is ejb-jar.xml, then this method looks for ejb-jar.xml under
     * org/jboss/ejb3/nointerface/integration/test/simple/unit in the classpath.
     * <p/>
     * Note: The list returned by the method could contain null elements if the
     * corresponding resource wasn't found in the classpath.
     *
     * @param klass     The Class under whose package the resources are searched for
     * @param resources The name of the resources
     * @return Returns a list of resource URLs
     */
    protected static List<URL> getResources(Class<?> klass, String... resources) {
        List<URL> urls = new ArrayList<URL>();
        for (String resource : resources) {
            urls.add(getResource(klass, resource));
        }
        return urls;
    }

    /**
     * Creates a jar file with the name <code>jarName</code> including in that jar, the packages
     * <code>testArtifactPackages</code>, and classes under those packages.
     *
     * @param jarName              Name of the jar file (including the .jar extension)
     * @param testArtifactPackages The test packages
     * @return Returns the created jar file
     * @throws IOException
     */
    protected static File buildSimpleJar(String jarName, Package... testArtifactPackages) throws IOException {
        JavaArchive jar = Archives.create(jarName, JavaArchive.class);
        jar.addPackages(false, testArtifactPackages);
        // also add any files from test/resources/<packagename>/*
        for (Package testPackage : testArtifactPackages) {
            if (TEST_SRC_RESOURCES.exists()) {
                //replace the "." with "/" in the packagename
                String resourceRelativePath = testPackage.getName().replaceAll("\\.", "/");
                File resourceAbsolutePath = new File(TEST_SRC_RESOURCES, resourceRelativePath);
                if (resourceAbsolutePath.exists()) {
                    // add all files, folders, sub-folders under this location to the root of the archive
                    jar.as(ExplodedImporter.class).importDirectory(resourceAbsolutePath);
                }
            }
        }
        return writeToFileSystem(jar);
    }

    /**
     * Creates a ear file with the name <code>earName</code>. The .ear will include the
     * (at the root) the files passed through the <code>deployments</code> param.
     *
     * @param earName     Name of the ear file (including the .ear extension)
     * @param deployments The files to be included in the .ear
     * @return Returns the created ear file
     * @throws IOException
     */
    protected static File buildSimpleEAR(String earName, File... deployments) throws IOException,
            IllegalArgumentException {
        if (earName == null || deployments == null) {
            throw new IllegalArgumentException("earName and deployments are mandatory");
        }
        JavaArchive ear = Archives.create(earName, JavaArchive.class);

        for (File deployment : deployments) {
            ear.addResource(deployment);
        }
        return writeToFileSystem(ear);
    }

    /**
     * Writes out a {@link JavaArchive} to the file system
     *
     * @param javaArchive The {@link JavaArchive} which will be written out to the file system
     * @return Returns the {@link File} corresponding to the {@link JavaArchive} which
     *         was written out to the file system
     * @throws IOException
     */
    protected static File writeToFileSystem(JavaArchive javaArchive) throws IOException {
        InputStream inputStream = javaArchive.as(ZipExporter.class).exportZip();
        String jarFileName = javaArchive.getName();
        logger.debug("Writing out jar " + jarFileName + " : " + javaArchive.toString(true));
        File jarFile = new File(TEST_DEPLOYMENTS_FOLDER, jarFileName);
        FileOutputStream fos = new FileOutputStream(jarFile);
        BufferedOutputStream bos = null;
        BufferedInputStream bis = null;
        try {
            bos = new BufferedOutputStream(fos);
            bis = new BufferedInputStream(inputStream);
            byte[] content = new byte[4096];
            int length;
            while ((length = bis.read(content)) != -1) {
                bos.write(content, 0, length);
            }
            bos.flush();
        } finally {
            if (bos != null) {
                bos.close();
            }
            if (bis != null) {
                bis.close();
            }
        }
        return jarFile;
    }

    /**
     * invoke wraps an invoke call to the mbean server in a lot of exception
     * unwrapping.
     *
     * @param name   ObjectName of the mbean to be called
     * @param method mbean method to be called
     * @param args   Object[] of arguments for the mbean method.
     * @param sig    String[] of types for the mbean methods parameters.
     * @return Object returned by mbean method invocation.
     * @throws Exception Description of Exception
     */
    protected Object invoke(ObjectName name, String method, Object[] args, String[] sig) throws Exception {
        return invoke(getServer(), name, method, args, sig);
    }

    protected Object invoke(MBeanServerConnection server, ObjectName name, String method, Object[] args, String[] sig)
            throws Exception {
        try {
            this.logger.debug("Invoking " + name.getCanonicalName() + " method=" + method);
            if (args != null)
                this.logger.debug("args=" + Arrays.asList(args));
            return server.invoke(name, method, args, sig);
        } catch (javax.management.MBeanException e) {
            logger.error("MbeanException", e.getTargetException());
            throw e.getTargetException();
        } catch (javax.management.ReflectionException e) {
            logger.error("ReflectionException", e.getTargetException());
            throw e.getTargetException();
        } catch (javax.management.RuntimeOperationsException e) {
            logger.error("RuntimeOperationsException", e.getTargetException());
            throw e.getTargetException();
        } catch (javax.management.RuntimeMBeanException e) {
            logger.error("RuntimeMbeanException", e.getTargetException());
            throw e.getTargetException();
        } catch (javax.management.RuntimeErrorException e) {
            logger.error("RuntimeErrorException", e.getTargetError());
            throw e.getTargetError();
        }
    }

    /**
     * Deploy a package with the main deployer. The supplied name is
     * interpreted as a url, or as a filename in jbosstest.deploy.lib or output/lib.
     *
     * @param name filename/url of package to deploy.
     * @throws Exception Description of Exception
     */
    public void deploy(URL deployURL) throws Exception {
        invoke(getDeployerName(), "deploy", new Object[]
                {deployURL}, new String[]
                {"java.net.URL"});
    }

    public void redeploy(URL deployURL) throws Exception {
        invoke(getDeployerName(), "redeploy", new Object[]
                {deployURL}, new String[]
                {"java.net.URL"});
    }

    private ObjectName getDeployerName() throws MalformedObjectNameException {
        return new ObjectName(DEPLOYER_NAME);
    }

    /**
     * Gets the Server attribute of the JBossTestCase object
     *
     * @return The Server value
     * @throws Exception for any error
     */
    protected MBeanServerConnection getServer() throws Exception {
        if (server == null) {
            String adaptorName = System.getProperty("jbosstest.server.name", "jmx/invoker/RMIAdaptor");
            server = (MBeanServerConnection) new InitialContext().lookup(adaptorName);
        }
        return server;
    }

    public void undeploy(URL deployURL) throws Exception {

        Object[] args =
                {deployURL};
        String[] sig =
                {"java.net.URL"};
        invoke(getDeployerName(), "undeploy", args, sig);
    }

    protected Context getInitialContext() {
        return this.ctx;
    }

}
