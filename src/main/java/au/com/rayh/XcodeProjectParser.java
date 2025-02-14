package au.com.rayh;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.text.ParseException;
import hudson.FilePath;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
//import javax.xml.parsers.ParserException;
import javax.xml.parsers.ParserConfigurationException;
import java.lang.InterruptedException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.dd.plist.NSDictionary; 
import com.dd.plist.NSArray;
import com.dd.plist.NSObject;
import com.dd.plist.PropertyListFormatException; 
import com.dd.plist.PropertyListParser; 

/**
 * Analyze the Xcode project file and obtain the information necessary for building the application. (Only combinations of UUID and BundleID used for CodeSign now)
 * @author Kazuhide Takahashi
 */
public class XcodeProjectParser {

    /**
     * Retrieve all Xcode scheme file from project directory.
     * @param projectLocation Xcode project file location (directory path)
     * @return the list of schema files found in the project directory and the result (ProjectScheme) of analysis of the contents as a HashMap. If analysis fails, it is empty
     */
    public static HashMap<String, ProjectScheme> listXcodeSchemes(FilePath projectLocation) {
	String currentUser = System.getProperty("user.name") ;
	HashMap<String, ProjectScheme> schemeList = new HashMap<String, ProjectScheme>();
	List<FilePath> schemeFilesDirList = new ArrayList<FilePath>();
	try {
	    if ( projectLocation.child("xcuserdata/" + currentUser + ".xcuserdatad/xcschemes").exists() ) {
		schemeFilesDirList.add(projectLocation.child("xcuserdata/" + currentUser + ".xcuserdatad/xcschemes"));
	    }
	    if ( projectLocation.child("xcshareddata/xcschemes").exists() ) {
		schemeFilesDirList.add(projectLocation.child("xcshareddata/xcschemes"));
	    }
	}
	catch ( IOException ex ) {
	    ex.printStackTrace();
	    schemeList = null;
	}
	catch ( InterruptedException ex ) {
	    ex.printStackTrace();
	    schemeList = null;
	}
	for ( FilePath schemeFilesDir : schemeFilesDirList ) {
	    try {
		List<FilePath> files = schemeFilesDir.list(new XcodeSchemeFileFilter());
		for ( FilePath file : files ) {
		    ProjectScheme scheme = parseXcodeScheme(file);
		    String schemeName = file.getBaseName().replaceAll(".xcscheme$", "");
		    schemeList.put(schemeName, scheme);
		}
	    }
	    catch ( IOException ex ) {
		ex.printStackTrace();
		schemeList = null;
	    }
	    catch ( InterruptedException ex ) {
		ex.printStackTrace();
		schemeList = null;
	    }
	}
	return schemeList;
    }

    /**
     * @param schemeFile Xcode schieme file location
     * @return analysis result of Xcode projectscheme file. If analysis fails, it is null
     */
    public static ProjectScheme parseXcodeScheme(FilePath schemeFile) {
	ProjectScheme projectScheme = new ProjectScheme();
	try {
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

	    String FEATURE = null;
	    FEATURE = "http://apache.org/xml/features/disallow-doctype-decl";
	    factory.setFeature(FEATURE, true);
	    FEATURE = "http://xml.org/sax/features/external-general-entities";
	    factory.setFeature(FEATURE, false);
	    FEATURE = "http://xml.org/sax/features/external-parameter-entities";
	    factory.setFeature(FEATURE, false);
	    FEATURE = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
	    factory.setFeature(FEATURE, false);
	    factory.setXIncludeAware(false);
	    factory.setExpandEntityReferences(false);

	    DocumentBuilder documentBuilder = factory.newDocumentBuilder();
	    Document document = documentBuilder.parse(schemeFile.read());

	    Element root = document.getDocumentElement();
	    if ( root.getNodeName().equals("Scheme") ) {
		NodeList schemeNodes = root.getChildNodes();
		for ( int i = 0; i < schemeNodes.getLength(); i++ ) {
		    Node node = schemeNodes.item(i);
		    if ( node.getNodeType() == Node.ELEMENT_NODE ) {
			Element element = (Element)node;
			if ( element.getNodeName().equals("BuildAction") ) {
			    //projectScheme.parallelizeBuildables = element.getAttribute("parallelizeBuildables");
			    //projectScheme.buildImplicitDependencies = element.getAttribute("buildImplicitDependencies");
			    NodeList buildActionNodes = element.getChildNodes();
			    for ( int j = 0; j < buildActionNodes.getLength(); j++ ) {
				node = buildActionNodes.item(j);
				if ( node.getNodeType() == Node.ELEMENT_NODE ) {
				    element = (Element)node;
				    if ( element.getNodeName().equals("BuildActionEntries") ) {
					NodeList buildActionEntriesNodes = element.getChildNodes();
					for ( int k = 0; k < buildActionEntriesNodes.getLength(); k++ ) {
					    node = buildActionEntriesNodes.item(k);
					    if ( node.getNodeType() == Node.ELEMENT_NODE ) {
						element = (Element)node;
						if ( element.getNodeName().equals("BuildActionEntry") ) {
						    //projectScheme.buildForTesting = element.getAttribute("buildForTesting");
						    //projectScheme.buildForRunning = element.getAttribute("buildForRunning");
						    //projectScheme.buildForProfiling = element.getAttribute("buildForProfiling");
						    //projectScheme.buildForArchiving = element.getAttribute("buildForArchiving");
						    //projectScheme.buildForAnalyzing = element.getAttribute("buildForAnalyzing");
						    NodeList buildActionEntryNodes = element.getChildNodes();
						    for ( int l = 0; l < buildActionEntryNodes.getLength(); l++ ) {
							node = buildActionEntryNodes.item(l);
							if ( node.getNodeType() == Node.ELEMENT_NODE ) {
							    element = (Element)node;
							    if ( element.getNodeName().equals("BuildableReference") ) {
								//projectScheme.buildableIdentifier = element.getAttribute("BuildableIdentifier");
								//projectScheme.blueprintIdentifier = element.getAttribute("BlueprintIdentifier");
								//projectScheme.buildableName = element.getAttribute("BuildableName");
								projectScheme.blueprintName = element.getAttribute("BlueprintName");
								projectScheme.referencedContainer = element.getAttribute("ReferencedContainer");
							    }
							}
						    }
						}
					    }
					}
				    }
				}
			    }
			}
		    }
		}
	    }
	}
	catch (SAXException ex) {
	    ex.printStackTrace();
	    projectScheme = null;
	}
	catch (IOException ex) {
	    ex.printStackTrace();
	    projectScheme = null;
	}
	catch (ParserConfigurationException ex) {
	    ex.printStackTrace();
	    projectScheme = null;
	}
        catch ( InterruptedException ex ) {
            ex.printStackTrace();
            projectScheme = null;
        }
	return projectScheme;
    }
    
    /**
     * @param workspaceFileLocation Xcode workspace file location (directory)
     * @return list of project files obtained as a result of analyzing workspaceFile. If analysis fails, it is empty
     */
    public static List<String> parseXcodeWorkspace(FilePath workspaceFileLocation) {
	List<String> projectList = new ArrayList<>();
	try {
	    FilePath workspaceFilePath = workspaceFileLocation.child("contents.xcworkspacedata");
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

	    String FEATURE = null;
	    FEATURE = "http://apache.org/xml/features/disallow-doctype-decl";
	    factory.setFeature(FEATURE, true);
	    FEATURE = "http://xml.org/sax/features/external-general-entities";
	    factory.setFeature(FEATURE, false);
	    FEATURE = "http://xml.org/sax/features/external-parameter-entities";
	    factory.setFeature(FEATURE, false);
	    FEATURE = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
	    factory.setFeature(FEATURE, false);
	    factory.setXIncludeAware(false);
	    factory.setExpandEntityReferences(false);

	    DocumentBuilder documentBuilder = factory.newDocumentBuilder();
	    Document document = documentBuilder.parse(workspaceFilePath.read());

	    Element root = document.getDocumentElement();
	    if ( root.getNodeName().equals("Workspace") ) {
		NodeList rootChildren = root.getChildNodes();

		for ( int i = 0; i < rootChildren.getLength(); i++ ) {
		    Node node = rootChildren.item(i);
		    if ( node.getNodeType() == Node.ELEMENT_NODE ) {
			Element element = (Element)node;
			if ( element.getNodeName().equals("FileRef") ) {
			    String projectLocation = element.getAttribute("location");
			    if ( projectLocation.startsWith("group:") ) {
				projectLocation = projectLocation.replaceAll("^group:", "");
				projectList.add(projectLocation);
			    }
			}
		    }
		}
	    }
	}
	catch (SAXException ex) {
	    ex.printStackTrace();
	    projectList = null;
	}
	catch (IOException ex) {
	    ex.printStackTrace();
	    projectList = null;
	}
	catch (ParserConfigurationException ex) {
	    ex.printStackTrace();
	    projectList = null;
	}
        catch ( InterruptedException ex ) {
            ex.printStackTrace();
            projectList = null;
        }
	return projectList;
    }

    /**
     * @param infoPlistFile Xcode Info.plist file location
     * @return analysis result of Info.plist file. If analysis fails, it is null
     */
    public static InfoPlist parseInfoPlist(FilePath infoPlistFile) {
	InfoPlist infoPlist = null;
	try {
	    NSDictionary rootDict = (NSDictionary)PropertyListParser.parse(infoPlistFile.read());
	    String cfBundleIdentifier = rootDict.objectForKey("CFBundleIdentifier").toString();
	    String cfBundleVersion = rootDict.objectForKey("CFBundleVersion").toString();
	    String cfBundleShortVersionString = rootDict.objectForKey("CFBundleShortVersionString").toString();
	    infoPlist = new InfoPlist(infoPlistFile, cfBundleIdentifier, cfBundleVersion, cfBundleShortVersionString);
	}
	catch ( Exception ex ) {
	    ex.printStackTrace();
	}
	return infoPlist;
    }

    /**
     * @param projectLocation Xcode project file location (directory)
     * @return analysis result of Xcode project file. If analysis fails, it is null
     */
    public static XcodeProject parseXcodeProject(FilePath projectLocation) {
	XcodeProject project = new XcodeProject();
	FilePath xcodeProjectFile = projectLocation.child("project.pbxproj");
	try {
	    NSDictionary rootDict = (NSDictionary)PropertyListParser.parse(xcodeProjectFile.read());
	    String rootObjectsUUID = rootDict.objectForKey("rootObject").toString();
	    NSDictionary objectsDict = ((NSDictionary)rootDict.objectForKey("objects"));
	    NSDictionary pbxProjectSectionDict = ((NSDictionary)objectsDict.objectForKey(rootObjectsUUID));
	    NSObject[] projectTargetUUIDs = ((NSArray)pbxProjectSectionDict.objectForKey("targets")).getArray();
	    // In case Project has buildConfigurationList.
	    // Parse each targets.
	    for ( NSObject projectTargetUUID:projectTargetUUIDs ) {
		ProjectTarget target = new ProjectTarget();
		NSDictionary projectTargetDict = ((NSDictionary)objectsDict.objectForKey(projectTargetUUID.toString()));
		target.uuid = projectTargetUUID.toString();
		String targetName = projectTargetDict.objectForKey("name").toString();
		// Target has buildConfigurationList.
		String buildConfigurationListUUID = projectTargetDict.objectForKey("buildConfigurationList").toString();
		target.productType = projectTargetDict.objectForKey("productType").toString();
		if ( target.productType.equals("com.apple.product-type.application") ||
		     target.productType.equals("com.apple.product-type.bundle.unit-test") ||
		     target.productType.equals("com.apple.product-type.bundle.ui-testing") ||
		     target.productType.equals("com.apple.product-type.watchkit-extension") ||
		     target.productType.equals("com.apple.product-type.application.watchapp") ||
		     target.productType.equals("com.apple.product-type.watchkit2-extension") ||
		     target.productType.equals("com.apple.product-type.application.watchapp2") ) {
		    NSDictionary attributesDict = ((NSDictionary)pbxProjectSectionDict.objectForKey("attributes"));
		    NSDictionary targetAttributesDict = ((NSDictionary)attributesDict.objectForKey("TargetAttributes"));
		    NSDictionary attributeDict = ((NSDictionary)targetAttributesDict.objectForKey(target.uuid));
		    if ( attributeDict.objectForKey("ProvisioningStyle") != null ) {
			target.provisioningStyle = attributeDict.objectForKey("ProvisioningStyle").toString();
		    }
		    else {
			// Default code signing style is "Automatic"
			target.provisioningStyle = "Automatic";
		    }
                    if ( attributeDict.objectForKey("TestTargetID") != null ) {
                        target.testTargetID = attributeDict.objectForKey("TestTargetID").toString();
                    }
		    NSDictionary buildConfigurationList = ((NSDictionary)objectsDict.objectForKey(buildConfigurationListUUID));
		    if ( buildConfigurationList != null ) {
			if ( buildConfigurationList.objectForKey("defaultConfigurationName") != null ) {
			    target.defaultConfigurationName = buildConfigurationList.objectForKey("defaultConfigurationName").toString();
			}
			NSObject[] buildConfigurationUUIDs = ((NSArray)buildConfigurationList.objectForKey("buildConfigurations")).getArray();
			// Parse each build configurations.
			for ( NSObject buildConfigurationUUID : buildConfigurationUUIDs ) {
			    BuildConfiguration buildConfiguration = new BuildConfiguration(objectsDict, buildConfigurationUUID.toString(), target.provisioningStyle.equals("Automatic"));
			    target.buildConfiguration.put(buildConfiguration.name, buildConfiguration);
			}
		    }
		    project.projectTarget.put(targetName, target);
		}
		else if ( target.productType.equals("com.apple.product-type.framework" ) ) {
		}
	    }
	}
	catch ( IOException ex ) {
	    ex.printStackTrace();
	    project = null;
	}
        catch ( PropertyListFormatException ex ) {
            ex.printStackTrace();
            project = null;
        }
        catch ( ParseException ex ) {
            ex.printStackTrace();
            project = null;
        }
        catch ( ParserConfigurationException ex ) {
            ex.printStackTrace();
            project = null;
        }
        catch ( SAXException ex ) {
            ex.printStackTrace();
            project = null;
        }
	catch ( InterruptedException ex ) {
	    ex.printStackTrace();
	    project = null;
	}
	return project;
    }
}
