import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.slf4j.Logger;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.WriteMode;
import com.thingworx.datashape.DataShape;
import com.thingworx.entities.utils.EntityUtilities;
import com.thingworx.logging.LogUtilities;
import com.thingworx.metadata.annotations.ThingworxBaseTemplateDefinition;
import com.thingworx.metadata.annotations.ThingworxConfigurationTableDefinition;
import com.thingworx.metadata.annotations.ThingworxConfigurationTableDefinitions;
import com.thingworx.metadata.annotations.ThingworxDataShapeDefinition;
import com.thingworx.metadata.annotations.ThingworxFieldDefinition;
import com.thingworx.metadata.annotations.ThingworxServiceDefinition;
import com.thingworx.metadata.annotations.ThingworxServiceParameter;
import com.thingworx.metadata.annotations.ThingworxServiceResult;
import com.thingworx.relationships.RelationshipTypes;
import com.thingworx.relationships.RelationshipTypes.ThingworxRelationshipTypes;
import com.thingworx.things.Thing;
import com.thingworx.things.repository.FileRepositoryThing;
import com.thingworx.types.InfoTable;
import com.thingworx.types.collections.ValueCollection;

@SuppressWarnings("serial")
@ThingworxBaseTemplateDefinition(name = "GenericThing")
	@ThingworxConfigurationTableDefinitions(tables = {
		@ThingworxConfigurationTableDefinition(name = "confDropbox", description = "", isMultiRow = false, ordinal = 0, dataShape = @ThingworxDataShapeDefinition(fields = {
			@ThingworxFieldDefinition(name = "accesstoken", description = "", baseType = "STRING", ordinal = 0) })) })

    public class DropBoxExtensionTemplate extends Thing {

	private static Logger _logger = LogUtilities.getInstance().getApplicationLogger(DropBoxExtensionTemplate.class);

	@ThingworxServiceDefinition(name = "GetDropboxList", description = "This service allows you to list the contents of your dropbox account using the dropbox account's access token.", category = "", isAllowOverride = false, aspects = {
	"isAsync:false" })
	
	@ThingworxServiceResult(name = "result", description = "", baseType = "INFOTABLE", aspects = {
	"isEntityDataShape:true", "dataShape:DropBoxDS" })
			
	public InfoTable GetDropboxList(@ThingworxServiceParameter(name = "Path", description = "", baseType = "STRING", aspects = {
			"isRequired:true", "defaultValue:" }) String Path) throws Exception {
			_logger.trace("Entering Service: GetDropboxList");
		ValueCollection entry = new ValueCollection();
		DataShape dataShape = (DataShape)EntityUtilities.findEntityDirect("DropBoxDS", RelationshipTypes.ThingworxRelationshipTypes.DataShape);
		InfoTable result = new InfoTable(dataShape.getDataShape());
		@SuppressWarnings("deprecation")
		DbxRequestConfig config = new DbxRequestConfig("dropbox/java-tutorial", "en_US");
		DbxClientV2 myClient = new DbxClientV2(config, (String) this.getConfigurationTable("confDropbox").getReturnValue());
		try {
			ListFolderResult resultt = myClient.files().listFolder(Path);
	            for (Metadata metadata : resultt.getEntries()) {
	               try {
	       			entry.clear();
	       			entry.SetStringValue("Name", metadata.getPathDisplay());
	       			result.addRow(entry.clone());
	       		}
	       		   catch (Exception e) {
	                   e.printStackTrace();
	               }
	            }  
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		_logger.trace("Exiting Service: GetDropboxList");
		return result;
	}

	@ThingworxServiceDefinition(name = "UploadFile", description = "", category = "", isAllowOverride = false, aspects = {
			"isAsync:false" })
	@ThingworxServiceResult(name = "Result", description = "", baseType = "STRING", aspects = {})
	public String UploadFile(
			@ThingworxServiceParameter(name = "Repository", description = "", baseType = "THINGNAME", aspects = {
			"thingTemplate:FileRepository" }) String Repository,
			@ThingworxServiceParameter(name = "DropBoxPath", description = "", baseType = "STRING", aspects = {
					"isRequired:true", "defaultValue:/" }) String DropBoxPath,
			@ThingworxServiceParameter(name = "FilePath", description = "", baseType = "STRING", aspects = {
					"isRequired:true", "defaultValue:\\<yourfilename>" }) String FilePath) throws Exception {
		_logger.trace("Entering Service: UploadFile");
		@SuppressWarnings("deprecation")
		DbxRequestConfig config = new DbxRequestConfig("dropbox/java-tutorial", "en_US");
		DbxClientV2 client = new DbxClientV2(config, (String) this.getConfigurationTable("confDropbox").getReturnValue()); 
		String result=null;
        FileRepositoryThing repo = (FileRepositoryThing) EntityUtilities.findEntity(Repository, ThingworxRelationshipTypes.Thing);
		String Repository1 = repo.getRootPath();
		File fileToUpload = new File(Repository1 + FilePath);
		try
        {
        	InputStream fileupload = new FileInputStream(fileToUpload);
            client.files().uploadBuilder(DropBoxPath + fileToUpload.getName())
        			.withMode(WriteMode.OVERWRITE)
                    .uploadAndFinish(fileupload);
           result = "File uploaded to dropbox";
        }
        catch (DbxException e)
        {
            result = ("Unable to upload file to Cloud \n Error: " + e);
        }
        catch (IOException e)
        {
            result = ("Unable to upload file to cloud \n Error: " + e);
        }
		_logger.trace("Exiting Service: UploadFile");
		return result;
	}

	@ThingworxServiceDefinition(name = "DownloadFile", description = "", category = "", isAllowOverride = false, aspects = {
			"isAsync:false" })
	@ThingworxServiceResult(name = "Result", description = "", baseType = "STRING", aspects = {})
	public String DownloadFile(
			@ThingworxServiceParameter(name = "SourcePath", description = "", baseType = "STRING", aspects = {
					"isRequired:true", "defaultValue:/<file path on your dropbox>" }) String SourcePath,
			@ThingworxServiceParameter(name = "FileName", description = "", baseType = "STRING", aspects = {
					"isRequired:true", "defaultValue:/file name>" }) String FileName,
			@ThingworxServiceParameter(name = "FileRepo", description = "", baseType = "THINGNAME", aspects = {
					"thingTemplate:FileRepository" }) String FileRepo,
			@ThingworxServiceParameter(name = "FolderPath", description = "", baseType = "STRING", aspects = {
					"isRequired:true", "defaultValue:\\<yourfolderpath>" }) String FolderPath) throws Exception {
		_logger.trace("Entering Service: DownloadFile");
		@SuppressWarnings("deprecation")
		DbxRequestConfig config = new DbxRequestConfig("dropbox/java-tutorial", "en_US");
		DbxClientV2 myClient = new DbxClientV2(config, (String) this.getConfigurationTable("confDropbox").getReturnValue());
		String result = null;
		FileRepositoryThing repo = (FileRepositoryThing) EntityUtilities.findEntity(FileRepo, ThingworxRelationshipTypes.Thing);
		String FileRepo1 = repo.getRootPath();
		Files.createDirectories(Paths.get(FileRepo1 + FolderPath));
		try
        {
            OutputStream downloadFile = new FileOutputStream(FileRepo1 + FolderPath + FileName);
            try
            {
			FileMetadata metadata = myClient.files().downloadBuilder(SourcePath)
                    .download(downloadFile);
			result = "File downloaded from dropbox";
            }
            finally
            {
            	downloadFile.close();
            }
        }
        catch (DbxException e)
        {
            File downloadFile = new File(FileRepo1 + FolderPath + FileName);
            downloadFile.delete();
            String directoryPath = (FileRepo1 + FolderPath);
            File dir = new File(directoryPath);
            dir.delete();
            result = ("Unable to download file to local system\n Error: " + e);
        }
        catch (IOException e)
        {
            File downloadFile = new File(FileRepo1 + FolderPath + FileName);
            downloadFile.delete();
            String directoryPath = (FileRepo1 + FolderPath);
            File dir = new File(directoryPath);
            dir.delete();
            result = ("Unable to download file to local system\n Error: " + e);
        }
		_logger.trace("Exiting Service: DownloadFile");
		return result;
	}
   	
}



