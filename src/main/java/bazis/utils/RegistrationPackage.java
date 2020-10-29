package bazis.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.io.FileUtils;
import sx.admin.AdmApplication;
import sx.admin.AdmRequest;
import sx.admin.AdmUtilDispatchAction;
import sx.cms.CmsApplication;
import sx.common.SXSession;
import sx.common.replication.DoReplication;
import sx.common.replication.PatchConfig;
import sx.datastore.*;
import sx.datastore.db.SXDb;
import sx.datastore.db.SXResultSet;
import sx.datastore.params.SXObjListParams;
import sx.sec.SXLogin;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;
import java.util.zip.ZipFile;

public class RegistrationPackage extends AdmUtilDispatchAction {
    private static final Charset CHCR = Charset.forName("CP1251");
    private static final Logger LOGGER = Logger.getLogger(RegistrationPackage.class.getName());

    @Override
    public void defaultCmd(AdmRequest admReq, AdmApplication admApp) throws Exception {
        SXAttrSearchFolder link = SXAttrSearchFolderUtils.getNode(admReq.getParam("link"));
        Map<String, Object> attrValues = link.getAttrValues();
        if (!attrValues.isEmpty()) {
            Object region = attrValues.get("region");
            SXId sxId = new SXId(region.toString());
            admReq.set("idToSend", sxId.getShortLink());
        } else {
            admReq.set("idToSend", "default");
        }
        includeTemplate("/WEB-INF/cms/admin/util/registrationpackage/registrationpackage", admReq);
    }


    public void init(AdmRequest admReq, AdmApplication admApp) throws Exception {
        String activeRootId = admReq.getParam("activeRootId");
        StringBuilder dropFolder = new StringBuilder();


        StringBuilder sqlAuthorBuilder = new StringBuilder();
        sqlAuthorBuilder.append("SELECT OUID, DESCRIPTION FROM SXUSER WHERE A_BLOCKED = 0 AND DESCRIPTION IS NOT NULL AND LOGIN NOT IN ('sa') ORDER BY DESCRIPTION;");
        SXResultSet executeQuery = null;
        SXDb db = SXDsFactory.getDs().getDb();
        String shortLink = "";


        SXLogin login = SXSession.getSXSession().getLogin();
        if (login != null && !"sa".equalsIgnoreCase(login.getName())) {
            shortLink = login.getUserId().getShortLink();
        }

        try {
            executeQuery = db.executeQuery(sqlAuthorBuilder.toString());
            sqlAuthorBuilder = new StringBuilder();
            sqlAuthorBuilder.append("<select class='form-control' id='authorSelect' style='cursor: pointer;'>");
            while (executeQuery.next()) {

                if (new SXLogin(new SXId(executeQuery.getString("OUID") + "@SXUser")).hasGroup("10403946")) continue;

                if (executeQuery.getString("OUID").equals(shortLink)) {
                    sqlAuthorBuilder
                            .append("<option selected authorId='")
                            .append(executeQuery.getString("OUID"))
                            .append("'>");
                } else {
                    sqlAuthorBuilder
                            .append("<option authorId='")
                            .append(executeQuery.getString("OUID"))
                            .append("'>");
                }
                sqlAuthorBuilder
                        .append(executeQuery.getString("DESCRIPTION"))
                        .append("</option>");
            }
            sqlAuthorBuilder.append("</select>");
            admReq.set("author", sqlAuthorBuilder.toString());

        } catch (Exception e) {
            LOGGER.warning(e.getMessage());
        } finally {
            if (executeQuery != null) {
                executeQuery.close();
            }
        }


        StringBuilder sqlRegSplit = new StringBuilder();

        sqlRegSplit.append("SELECT SS.OUID, SS.A_NAME, RI.A_RAION_NAME, RI.A_IP_ADRESS_RAION, RI.A_REGION, RI.A_VID_SERVER, RI.A_OUID FROM REFERENCE_INF RI JOIN SPR_SUBJFED SS ON SS.OUID = RI.A_REGION WHERE A_VID_SERVER IN (10,9,1)");

        HashMap<String, ArrayList<String>> resultAllMap = new HashMap<>();

        try {
            executeQuery = db.executeQuery(sqlRegSplit.toString());

            while (executeQuery.next()) {

                String region = executeQuery.getString("A_REGION");
                String rayon = executeQuery.getString("A_RAION_NAME");
                String ipAddress = executeQuery.getString("A_IP_ADRESS_RAION");
                String rayonId = executeQuery.getString("A_OUID");
                String vid = executeQuery.getString("A_VID_SERVER");

                ArrayList<String> get = resultAllMap.get(region);
                if (get != null) {
                    if (!get.contains(rayon)) {
                        get.add(vid + ";" + rayon + ";" + ipAddress + ";" + rayonId);
                    }
                } else {
                    get = new ArrayList<>();
                    get.add(vid + ";" + rayon + ";" + ipAddress + ";" + rayonId);

                }
                resultAllMap.put(region, get);
            }

            StringBuilder tabs = new StringBuilder();
            StringBuilder contentOther = new StringBuilder();
            StringBuilder contentTest = new StringBuilder();
            StringBuilder contentWork = new StringBuilder();

            boolean flagContent = false;

            String vid;
            String rayName;
            String ipAddress;
            String rayNameId;

            Map<String, ArrayList<String>> sortMap = new TreeMap<>(resultAllMap);

            File formDropSelect = new File(Objects.requireNonNull(CmsApplication.getCmsApplication().getRealPath("/files/universalInfoobmen/copyFolder/pack/")));


            tabs
                    .append("<div class='nav flex-column nav-pills' role='tablist' aria-orientation ='vertical' id='pills-tab'>");

            contentWork
                    .append("<div class='tab-content' id='pills-tabContentOther'>");

            dropFolder
                    .append("<div class='folder-content' id='pills-folderContent'>");

            for (Map.Entry<String, ArrayList<String>> resMapEntry : sortMap.entrySet()) {

                SXObjListParams listParams = new SXObjListParams("sprSubjectFederation");
                listParams.addSelectedAttr("name");
                listParams.addCondition("ouid", resMapEntry.getKey());
                listParams.setGetLinkedObjects(false);
                listParams.getObj();
                SXObj obj = listParams.getObj();
                String regionName = null;


                Map<String, List<String>> strings = readFileFolder(formDropSelect, resMapEntry.getKey());

                if (obj != null) {
                    regionName = obj.getStringAttr("name");
                }

                if (resMapEntry.getKey().equals(activeRootId)) {
                    tabs
                            .append("<a class='nav-link active' data-toggle='pill' role='tab' style='text-align: center; letter-spacing: 1px; font-family: Apple Color emoji; padding: .4rem;' aria-selected='true' id='v-");
                    flagContent = true;
                } else if (activeRootId.equals("default") && resMapEntry.getKey().equals("661")) {
                    tabs
                            .append("<a class='nav-link active' data-toggle='pill' role='tab' style='text-align: center; letter-spacing: 1px; font-family: Apple Color emoji; padding: .4rem;' aria-selected='true' id='v-");
                    flagContent = true;
                } else {
                    tabs
                            .append("<a class='nav-link' data-toggle='pill' role='tab' style='text-align: center; letter-spacing: 1px; font-family: Apple Color emoji; padding: .4rem;' aria-selected='false' id='v-");
                }
                tabs
                        .append(resMapEntry.getKey())
                        .append("-tab'")
                        .append(" name='v-")
                        .append(resMapEntry.getKey())
                        .append("-tab'")
                        .append(" aria-controls='v-")
                        .append(resMapEntry.getKey())
                        .append("-content'")
                        .append(" href='#v-")
                        .append(resMapEntry.getKey())
                        .append("-content'")
                        .append(" regThisId ='")
                        .append(resMapEntry.getKey())
                        .append("' regionTabName='")
                        .append(regionName)
                        .append("'>")
                        .append(regionName)
                        .append("</a>");

                dropFolder
                        .append("<div class='folderShowContent' role='tabpanel_folder' style='display: none;' id='");


                if (flagContent) {
                    contentWork
                            .append("<div class='tab-pane fade active show' role='tabpanel' id='v-");
                    flagContent = false;
                } else {
                    contentWork
                            .append("<div class='tab-pane fade' role='tabpanel' id='v-");
                }

                contentWork
                        .append(resMapEntry.getKey())
                        .append("-content'")
                        .append(" aria-labelledby='v-")
                        .append(resMapEntry.getKey())
                        .append("-tab'>")
                        .append("<div style='display: inline-flex;'>")
                        .append("<div class='col' id='workCol' style='border: ridge; border-color: #c0f6ff7a; border-radius: 13px; margin-left: 10px; overflow-y: scroll; height: 50rem; position: relative; width: 20rem;'>")
                        .append("<p style='border-bottom: 2px solid; border-color: #c0f6ff7a; border-bottom-style: ridge; border-radius: 0 0 50px 50px; text-align: center; font-size: 1.3rem; font-family: Apple Color emoji;'>")
                        .append("Рабочие")
                        .append("</p>");

                dropFolder
                        .append(resMapEntry.getKey())
                        .append("-folder'>");


                for (Map.Entry<String, List<String>> entry : strings.entrySet()) {
                    dropFolder
                            .append("<div id='")
                            .append(entry.getKey())
                            .append("'>")
                            .append("<select class='form-control' style='cursor: pointer;' id='folderSelect-")
                            .append(resMapEntry.getKey())
                            .append("' name='folderSelect-")
                            .append(resMapEntry.getKey())
                            .append("'>")
                            .append("<option selected disabled selected hidden>")
                            .append("Выбрать существующую директорию")
                            .append("</option>");

                    for (String folder : entry.getValue()) {
                        dropFolder
                                .append("<option>")
                                .append(folder)
                                .append("</option>");

                    }

                    dropFolder
                            .append("</select>")
                            .append("</div>");
                }
                dropFolder
                        .append("</div>");


                contentTest
                        .append("<div class='col' id='testCol' style='border: ridge; border-color: #a1b4fb87; border-radius: 13px; margin-left: 10px; overflow-y: scroll; height: 50rem; position: relative; width: 25rem;'>")
                        .append("<p style='border-bottom: 2px solid; border-color: #a1b4fb87; border-bottom-style: ridge; border-radius: 0 0 50px 50px; text-align: center; font-size: 1.3rem; font-family: Apple Color emoji;'>")
                        .append("Тестовые")
                        .append("</p>");

                contentOther
                        .append("<div class='col' id='otherCol' style='border: ridge; border-color: #bbef9b7a; border-radius: 13px; margin-left: 10px; overflow-y: scroll; height: 50rem; position: relative; width: 30rem;'>")
                        .append("<p style='border-bottom: 2px solid; border-color: #bbef9b7a; border-bottom-style: ridge; border-radius: 0 0 50px 50px; text-align: center; font-size: 1.3rem; font-family: Apple Color emoji;'>")
                        .append("Прочие")
                        .append("</p>");

                for (String resStr : resMapEntry.getValue()) {
                    String[] split = resStr.split(";");
                    vid = null;
                    rayName = null;
                    ipAddress = null;
                    rayNameId = null;
                    for (String currValue : split) {
                        if (vid == null) {
                            vid = currValue;
                        } else if (rayName == null) {
                            rayName = currValue;
                        } else if (ipAddress == null) {
                            ipAddress = currValue;
                        } else {
                            rayNameId = currValue;
                        }
                    }

                    if (vid != null) {
                        if (vid.equals("1")) {
                            contentWork
                                    .append("<div class='form-check' style='user-select: none; margin: .2rem!important; border-bottom: 1px solid #63a8b3; border-radius: 10px;'>")
                                    .append("<input class='form-check-input' name='selectAllCheck' type='checkbox' style='width: 30px;' id='")
                                    .append(rayNameId)
                                    .append("'>")
                                    .append("<label class='form-check-label' style='margin-left: 15px; width: 85%;  cursor: pointer;' for='")
                                    .append(rayNameId)
                                    .append("' title='")
                                    .append(ipAddress)
                                    .append("'>")
                                    .append(rayName)
                                    .append("</label>")
                                    .append("<label style='vertical-align: top;' hidden>")
                                    .append(rayNameId)
                                    .append("</label>")
                                    .append("</div>");

                        } else if (vid.equals("9")) {
                            contentTest
                                    .append("<div class='form-check' style='user-select: none; margin: .2rem!important; border-bottom: 1px solid #9cb0fd; border-radius: 10px;'>")
                                    .append("<input class='form-check-input' name='selectAllCheck' type='checkbox' style='width: 30px;' id='")
                                    .append(rayNameId)
                                    .append("'>")
                                    .append("<label class='form-check-label' style='margin-left: 15px; width: 85%;  cursor: pointer;' for='")
                                    .append(rayNameId)
                                    .append("' title='")
                                    .append(ipAddress)
                                    .append("'>")
                                    .append(rayName)
                                    .append("</label>")
                                    .append("<label style='vertical-align: top;' hidden>")
                                    .append(rayNameId)
                                    .append("</label>")
                                    .append("</div>");
                        } else {
                            contentOther
                                    .append("<div class='form-check' style='user-select: none; margin: .2rem!important; border-bottom: 1px solid #adce97; border-radius: 10px;'>")
                                    .append("<input class='form-check-input' name='selectAllCheck' type='checkbox' style='width: 30px;' id='")
                                    .append(rayNameId)
                                    .append("'>")
                                    .append("<label class='form-check-label' style='margin-left: 15px; width: 85%;  cursor: pointer;' for='")
                                    .append(rayNameId)
                                    .append("' title='")
                                    .append(ipAddress)
                                    .append("'>")
                                    .append(rayName)
                                    .append("</label>")
                                    .append("<label style='vertical-align: top;' hidden>")
                                    .append(rayNameId)
                                    .append("</label>")
                                    .append("</div>");
                        }
                    }
                }

                contentTest

                        .append("</div>");

                contentOther
                        .append("</div>");

                contentWork
                        .append("</div>")
                        .append(contentTest.toString())
                        .append(contentOther.toString())
                        .append("</div>")
                        .append("</div>");

                contentOther = new StringBuilder();
                contentTest = new StringBuilder();

            }

            dropFolder
                    .append("</div>");

            tabs
                    .append("</div>");

            contentWork
                    .append("</div>");


            admReq.set("showFolderContent", dropFolder.toString());
            admReq.set("tabsRef", tabs.toString());
            admReq.set("contentRef", contentWork.toString());
        } catch (Exception e) {
            LOGGER.warning(e.getMessage());
        } finally {
            if (executeQuery != null) {
                executeQuery.close();
            }
        }
        includeTemplate("/WEB-INF/cms/admin/util/registrationpackage/init", admReq);
    }

    public Map<String, List<String>> readFileFolder(File baseFolder, String regionId) throws SQLException {
        Map<String, List<String>> result = new HashMap<>();
        String dateFormat = new SimpleDateFormat("/yyyy/MM/dd/").format(Calendar.getInstance().getTime());
        SXResultSet executeQuery = null;
        SXDb db = SXDsFactory.getDs().getDb();
        StringBuilder sqlTest = new StringBuilder();
        sqlTest
                .append("SELECT A_NAME FROM SPR_SUBJFED WHERE OUID = ")
                .append(regionId);
        try {
            executeQuery = db.executeQuery(sqlTest.toString());
            while (executeQuery.next()) {
                String regName = executeQuery.getString("A_NAME");
                LinkedList<String> listRegion = new LinkedList<>();
                File[] regionFolder = baseFolder.listFiles();
                if (regionFolder != null && regionFolder.length > 0) {
                    for (File entry : regionFolder) {
                        if (entry.isDirectory()) {
                            if (entry.getName().equals(regName)) {
                                //todo:Проверить соответствие наличия папки и региона
                                listRegion.add(entry.getName());
                            }
                        }
                    }
                }


                for (String region : listRegion) {
                    File patchNow = new File(baseFolder, region + dateFormat);
                    File[] files = patchNow.listFiles();
                    if (files != null && files.length > 0) {
                        List<String> list = new ArrayList<>();
                        for (File pathDir : files) {
                            if (pathDir.isDirectory()) {
                                String normalDirPath = pathDir.getAbsolutePath().replace(baseFolder.getAbsolutePath(), "");
                                normalDirPath = normalDirPath.startsWith("\\") ? normalDirPath.substring(1).replace("\\", "/") : normalDirPath;
                                list.add(normalDirPath);
                            }
                        }
                        if (!list.isEmpty()) {
                            result.put(region, list);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warning(e.getMessage());
        } finally {
            if (executeQuery != null) {
                executeQuery.close();
            }
        }


        return result;
    }


    public void testSending(AdmRequest admReq, AdmApplication admApp) throws Exception {
        StringBuilder successTableBuilder = new StringBuilder();

        String raiNameFolder = "";
        String raySelectedOuid = admReq.getParam("activeRaySelect");

        SXResultSet executeQuery = null;
        SXDb db = SXDsFactory.getDs().getDb();
        StringBuilder sqlRayFolder = new StringBuilder();
        sqlRayFolder
                .append("SELECT A_NAME FROM SPR_SUBJFED WHERE OUID = ")
                .append(admReq.getParam("activeTabSelect"));
        try {
            executeQuery = db.executeQuery(sqlRayFolder.toString());
            while (executeQuery.next()) {
                raiNameFolder = executeQuery.getString("A_NAME");
            }

            File uInfoobmenPath = new File(Objects.requireNonNull(CmsApplication.getCmsApplication()
                    .getRealPath(File.separator + "files" + File.separator + "universalInfoobmen")));

            String dateFormat = new SimpleDateFormat(File.separator + "yyyy" + File.separator + "MM" + File.separator + "dd" + File.separator)
                    .format(Calendar.getInstance().getTime());

            String installPackegePath = uInfoobmenPath.getAbsolutePath() + File.separator + "installPackege" + File.separator + "pack" + File.separator;

            String copyFolderPath = uInfoobmenPath.getAbsolutePath() + File.separator + "copyFolder" + File.separator + "pack" + File.separator;

            String comment = admReq.getParam("areaComment");
            String selectedAuthor = admReq.getParam("selectedAuthorId");
            String fileDescription = admReq.getParam("descriptionForFile");
            String region = admReq.getParam("activeTabSelect");
            String isUpdateCms = admReq.getParam("flagUpdateCms");
            String isRestart = admReq.getParam("flagRestart");

            //Проверка на файл ("1" - несколько файлов)
            if (admReq.getParamMap().get("flagFileOrDirectory").equals("1")) {
                File createActualDirectory = new File(installPackegePath + raiNameFolder + dateFormat);
                createActualDirectory.mkdirs();
                ArrayList<DiskFileItem> list = (ArrayList<DiskFileItem>) admReq.getParamMap().get("userFileUpload");
                for (DiskFileItem item : list) {
                    InputStream inputStreamFile = item.getInputStream();
                    String fileName = item.getName();
                    File file = new File(createActualDirectory.getAbsolutePath() + File.separator + fileName);
                    FileUtils.copyInputStreamToFile(inputStreamFile, file);

                    try (ZipFile zfile = new ZipFile(file, CHCR)) {
                        DoReplication doReplication = new DoReplication();
                        PatchConfig pConfig = doReplication.getPatchConfig(zfile, null);
                        String codePackage = pConfig.getCode();

                        executeQuery = db.executeQuery("SELECT NEWID() AS GUID");
                        String packGuid = "";
                        while (executeQuery.next()) {
                            packGuid = executeQuery.getString("GUID");
                        }

                        String changedPath = createActualDirectory.getAbsolutePath().replace(installPackegePath, "") + File.separator;

                        executeAddPack(fileName, codePackage, changedPath, comment, packGuid, selectedAuthor, fileDescription,
                                region, isUpdateCms, isRestart);

                        executeAddRaion(packGuid, raySelectedOuid);

                        if (admReq.getParam("dependentPackages") != null) {
                            setDependentPackage(admReq.getParam("dependentPackages"), packGuid);
                        }

                        successTableBuilder
                                .append(successJspBuild(packGuid, fileName, fileDescription).toString());

                    } catch (Exception e) {
                        LOGGER.warning(e.getMessage());
                    }
                }

                //Проверка на файл ("default" - один файл)
            } else if (admReq.getParamMap().get("flagFileOrDirectory").equals("default")) {
                try {
                    File createActualDirectory = new File(installPackegePath + raiNameFolder + dateFormat);
                    createActualDirectory.mkdirs();
                    InputStream inputStreamFile = ((DiskFileItem) admReq.getParamMap().get("userFileUpload")).getInputStream();
                    String fileName = ((DiskFileItem) admReq.getParamMap().get("userFileUpload")).getName();
                    File file = new File(createActualDirectory.getAbsolutePath() + File.separator + fileName);
                    FileUtils.copyInputStreamToFile(inputStreamFile, file);


                    try (ZipFile zfile = new ZipFile(file, CHCR)) {
                        DoReplication doReplication = new DoReplication();
                        PatchConfig pConfig = doReplication.getPatchConfig(zfile, null);
                        String codePackage = pConfig.getCode();

                        executeQuery = db.executeQuery("SELECT NEWID() AS GUID");
                        String packGuid = "";
                        while (executeQuery.next()) {
                            packGuid = executeQuery.getString("GUID");
                        }

                        String changedPath = createActualDirectory.getAbsolutePath().replace(installPackegePath, "") + File.separator;

                        executeAddPack(fileName, codePackage, changedPath, comment, packGuid, selectedAuthor, fileDescription,
                                region, isUpdateCms, isRestart);

                        executeAddRaion(packGuid, raySelectedOuid);

                        if (admReq.getParam("dependentPackages") != null) {
                            setDependentPackage(admReq.getParam("dependentPackages"), packGuid);
                        }

                        successTableBuilder
                                .append(successJspBuild(packGuid, fileName, fileDescription).toString());

                    } catch (Exception e) {
                        LOGGER.warning(e.getMessage());
                    }
                } catch (Exception e) {
                    LOGGER.warning(e.getMessage());
                }

                //Проверка на файл (Директория)
            } else {
                File directory;
                File file;
                File createActualDirectory = new File(copyFolderPath + raiNameFolder + dateFormat);
                createActualDirectory.mkdirs();

                GsonBuilder builder = new GsonBuilder().serializeNulls();
                Gson gs = builder.create();
                HashMap<String, Object> jsonObjInput = gs.fromJson(admReq.getParam("jsonObjInput"), HashMap.class);
                Map<String, Object> values = getValues(jsonObjInput);

                File replaceFile = new File(createActualDirectory + File.separator + jsonObjInput.keySet().iterator().next());

                String changedPath = createActualDirectory.getAbsolutePath().replace(copyFolderPath, "") + File.separator;

                for (Map.Entry<String, Object> valEntry : values.entrySet()) {
                    if (valEntry.getValue() instanceof ArrayList) {
                        String filePathDirectory = valEntry.getKey();
                        directory = new File(createActualDirectory.getAbsolutePath() + File.separator + filePathDirectory);
                        directory.mkdirs();
                        Iterator<LinkedTreeMap<String, String>> iter = ((ArrayList) valEntry.getValue()).iterator();
                        while (iter.hasNext()) {
                            LinkedTreeMap<String, String> next = iter.next();
                            String fileName = next.get("FileName");
                            String absolutePath = directory.getAbsolutePath();
                            file = new File(absolutePath + File.separator + fileName);
                            file.createNewFile();
                            InputStream inputStreamFile = ((DiskFileItem) admReq.getParamMap().get(next.get("id"))).getInputStream();
                            FileUtils.copyInputStreamToFile(inputStreamFile, file);
                        }
                    } else {
                        directory = new File(createActualDirectory.getAbsolutePath() + File.separator + valEntry.getKey());
                        directory.mkdirs();
                    }
                }
                try {
                    executeQuery = db.executeQuery("SELECT NEWID() AS GUID");
                    String packGuid = "";
                    while (executeQuery.next()) {
                        packGuid = executeQuery.getString("GUID");
                    }

                    executeAddPack(fileDescription, "", changedPath, comment, packGuid, selectedAuthor,
                            fileDescription, region, isUpdateCms, isRestart);

                    executeAddRaion(packGuid, raySelectedOuid);

                    if (admReq.getParam("dependentPackages") != null) {
                        setDependentPackage(admReq.getParam("dependentPackages"), packGuid);
                    }

                    successTableBuilder
                            .append(successJspBuild(packGuid, fileDescription, fileDescription).toString());

                } catch (Exception e) {
                    LOGGER.warning(e.getMessage());
                }
            }
        } catch (Exception e) {
            LOGGER.warning(e.getMessage());
        } finally {
            if (executeQuery != null) {
                executeQuery.close();
            }
        }
        admReq.set("showResultRegistration", successTableBuilder.toString());
        includeTemplate("/WEB-INF/cms/admin/util/registrationpackage/successfulReg", admReq);
    }

    public Map<String, Object> getValues(Map<String, Object> map) {
        Map<String, Object> newMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map) {
                //noinspection rawtypes
                Map valueMap = (Map) entry.getValue();
                if (valueMap.isEmpty()) {
                    newMap.put(entry.getKey(), "");
                } else {
                    Map<String, Object> tempMap = getValues(valueMap);
                    for (Map.Entry<String, Object> tempEntry : tempMap.entrySet()) {
                        newMap.put(entry.getKey() + File.separator + tempEntry.getKey(), tempEntry.getValue());
                    }
                }
            } else {
                if (entry.getKey().equals("|")) {
                    newMap.put("", entry.getValue());
                } else {
                    newMap.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return newMap;
    }

    public void executeAddPack(String fileName, String codePackage, String filePath, String comment, String packGuid,
                               String author, String description, String region, String isUpdateCms, String isRestart) {
        SXDb db = SXDsFactory.getDs().getDb();
        try {
            db.execute("addpack '" + fileName + "','" + codePackage + "','" + filePath
                    + "','" + comment + "','" + packGuid + "','" + author + "','" + description + "','" + region
                    + "','" + isUpdateCms + "','" + isRestart + "'");
        } catch (Exception e) {
            LOGGER.warning(e.getMessage());
        }
    }

    public void executeAddRaion(String packGuid, String rayons) {
        SXDb db = SXDsFactory.getDs().getDb();
        try {
            db.execute("addraion '" + packGuid + "','" + rayons + "'");
        } catch (Exception e) {
            LOGGER.warning(e.getMessage());
        }
    }

    public void setDependentPackage(String dependentPackage, String packGuid) {
        SXDb db = SXDsFactory.getDs().getDb();
        try {
            db.execute("UPDATE pack set A_PARENT = " + dependentPackage + " WHERE GUID = '" + packGuid + "'");
        } catch (Exception e) {
            LOGGER.warning(e.getMessage());
        }
    }

    public StringBuilder successJspBuild(String packGuid, String packageName, String description) throws Exception {
        SXResultSet executeQuery = null;
        SXDb db = SXDsFactory.getDs().getDb();

        String packOuid = "";
        String author = "";
        String date = "";
        StringBuilder jspBuilder = new StringBuilder();
        jspBuilder
                .append("SELECT Pck.OUID, SXUs.DESCRIPTION packAuthor, Pck.TS date FROM pack Pck JOIN SXUSER SXUs ON Pck.CROWNER = SXUs.OUID WHERE Pck.GUID = '")
                .append(packGuid)
                .append("'");

        try {
            executeQuery = db.executeQuery(jspBuilder.toString());
            while (executeQuery.next()) {
                packOuid = executeQuery.getString("OUID");
                author = executeQuery.getString("packAuthor");
                date = executeQuery.getString("date");
            }

            SimpleDateFormat oldDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            SimpleDateFormat newDateFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm");
            Date getExpectDate = oldDateFormat.parse(date);
            String convertDate = newDateFormat.format(getExpectDate);

            jspBuilder = new StringBuilder();

            jspBuilder
                    .append("<div class='row' style='width: 100%; margin-bottom: 5px;'>")
                    .append("<div class='col-sm-1' style='display: flex;  justify-content: center; align-items: center;'></div>")
                    .append("<div class='col-sm-1' style='text-align: center;padding: 0;display: flex;justify-content: center;align-items: flex-end;word-break: break-word;font-size: 14px;font-weight: 500;'>Ouid</div>")
                    .append("<div class='col-sm-3' style='text-align: center;display: flex;justify-content: center;align-items: flex-end;word-break: break-word;font-size: 14px;font-weight: 500;'>Наименование пакета</div>")
                    .append("<div class='col' style='text-align: center;display: flex;justify-content: center;align-items: flex-end;word-break: break-word;font-size: 14px;font-weight: 500;'>Описание</div>")
                    .append("<div class='col-sm-2' style='text-align: center;display: flex;justify-content: center;align-items: flex-end;word-break: break-word;font-size: 14px;font-weight: 500;'>Автор обновления</div>")
                    .append("<div class='col-sm-2' style='text-align: center;display: flex;justify-content: center;align-items: flex-end;word-break: break-word;font-size: 14px;font-weight: 500;'>Дата регистрации</div>")
                    .append("<div class='col-sm-1'></div>")
                    .append("</div>")

                    .append("<div class='row' style='width: 100%;' id='rowOuid-")
                    .append(packOuid)
                    .append("'>")
                    .append("<div class='col-sm-1' style='display: flex;  justify-content: center; align-items: center;'>")
                    .append("<i class='far fa-thumbs-up' style='color: #28a745; font-size: 25px;'></i>")
                    .append("</div>")
                    .append("<div class='col-sm-1 border border-success rounded-left border-right-0' style='text-align: center; padding: 0; display: flex; justify-content: center; align-items: center; word-break: break-word;' packOuid='")
                    .append(packOuid)
                    .append("'>")
                    .append(packOuid)
                    .append("</div>")
                    .append("<div class='col-sm-3 border border-success border-right-0' style='text-align: center; display: flex; justify-content: center; align-items: center; word-break: break-word;' packName='")
                    .append(packageName)
                    .append("'>")
                    .append(packageName)
                    .append("</div>")
                    .append("<div class='col border border-success border-right-0' style='text-align: center; display: flex; justify-content: center; align-items: center; word-break: break-word;' packDescr='")
                    .append(description)
                    .append("'>")
                    .append(description)
                    .append("</div>")
                    .append("<div class='col-sm-2 border border-success border-right-0' style='text-align: center; display: flex; justify-content: center; align-items: center; word-break: break-word;' packAuthor='")
                    .append(author)
                    .append("'>")
                    .append(author)
                    .append("</div>")
                    .append("<div class='col-sm-2 border border border-success rounded-right' style='text-align: center; display: flex; justify-content: center; align-items: center; word-break: break-word;' packDate='")
                    .append(convertDate)
                    .append("'>")
                    .append(convertDate)
                    .append("</div>")
                    .append("<div class='col-sm-1' style='display: flex; justify-content: center; align-items: center;'>")
                    .append("<button type='button' class='btn' style='border: 0; background: white;' onclick='copyTableContent(" + packOuid + ")' data-toggle='tooltip' data-placement='top' title='Скопировать' id='btn-")
                    .append(packOuid)
                    .append("'>")
                    .append("<i class='far fa-copy' style='font-size: 25px;'></i>")
                    .append("</button>")
                    .append("</div>")
                    .append("</div>")

                    .append("<div class='row' style='margin-bottom: 1rem; width: 100%;'>")
                    .append("<textarea class='form-control' type='text' style='resize: none; border: 0; background: white; pointer-events: none; box-shadow: none;' id='copyTextOf-")
                    .append(packOuid)
                    .append("' readonly></textarea>")
                    .append("</div>");
        } catch (Exception e) {
            LOGGER.warning(e.getMessage());
        } finally {
            if (executeQuery != null) {
                executeQuery.close();
            }
        }

        return jspBuilder;
    }
}
