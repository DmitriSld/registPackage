package bazis.utils;

import com.sun.deploy.net.HttpRequest;
import com.sun.deploy.net.HttpResponse;
import sx.admin.AdmApplication;
import sx.admin.AdmRequest;
import sx.admin.AdmUtilDispatchAction;
import sx.common.SXSession;
import sx.common.SXUtils;
import sx.datastore.*;
import sx.datastore.db.SXDb;
import sx.datastore.db.SXResultSet;
import sx.datastore.impl.sitex2.beans.SXTuneAttrSearch;
import sx.datastore.params.SXObjListParams;
import sx.sec.SXLogin;

import javax.servlet.http.HttpServlet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class RegistrationPackage extends AdmUtilDispatchAction {
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
        StringBuilder sqlAuthorBuilder = new StringBuilder();
        sqlAuthorBuilder.append("SELECT OUID, DESCRIPTION FROM SXUSER WHERE A_BLOCKED = 0 AND DESCRIPTION IS NOT NULL AND LOGIN NOT IN ('sa') ORDER BY DESCRIPTION;");
        SXResultSet executeQuery = null;
        SXDb db = SXDsFactory.getDs().getDb();
        String shortLink = "";

        String activeRootId = admReq.getParam("activeRootId");

        SXLogin login = SXSession.getSXSession().getLogin();
        if (!login.getSa()) {
            shortLink = login.getUserId().getShortLink();
        }

        try {
            executeQuery = db.executeQuery(sqlAuthorBuilder.toString());
            sqlAuthorBuilder = new StringBuilder();
            sqlAuthorBuilder.append("<select class='form-control' style='width: auto;'>");
            while (executeQuery.next()) {

                if (new SXLogin(new SXId(executeQuery.getString("OUID") + "@SXUser")).hasGroup("10403946")) continue;

                if (executeQuery.getString("OUID").equals(shortLink)) {
                    sqlAuthorBuilder
                            .append("<option selected>");
                } else {
                    sqlAuthorBuilder
                            .append("<option>");
                }
                sqlAuthorBuilder
                        .append(executeQuery.getString("DESCRIPTION"))
                        .append(" ")
                        .append(executeQuery.getString("OUID"))
                        .append("</option>");
            }
            sqlAuthorBuilder.append("</select>");
            admReq.set("author", sqlAuthorBuilder.toString());

        } catch (Exception e) {
            e.getCause();
        } finally {
            if (executeQuery != null) {
                executeQuery.close();
            }
        }


        StringBuilder sqlRegSplit = new StringBuilder();

        sqlRegSplit.append("SELECT SS.OUID, SS.A_NAME, RI.A_RAION_NAME, RI.A_REGION, RI.A_VID_SERVER, RI.A_OUID FROM REFERENCE_INF RI JOIN SPR_SUBJFED SS ON SS.OUID = RI.A_REGION WHERE A_VID_SERVER IN (10,9,1)");

        HashMap<String, ArrayList<String>> resultAllMap = new HashMap<String, ArrayList<String>>();

        try {
            executeQuery = db.executeQuery(sqlRegSplit.toString());

            while (executeQuery.next()) {

                String region = executeQuery.getString("A_REGION");
                String rayon = executeQuery.getString("A_RAION_NAME");
                String rayonId = executeQuery.getString("A_OUID");
                String vid = executeQuery.getString("A_VID_SERVER");

                ArrayList<String> get = resultAllMap.get(region);
                if (get != null) {
                    if (!get.contains(rayon)) {
                        get.add(vid + ";" + rayon + ";" + rayonId);
                    }
                } else {
                    get = new ArrayList<String>();
                    get.add(vid + ";" + rayon + ";" + rayonId);

                }
                resultAllMap.put(region, get);
            }

            StringBuilder tabs = new StringBuilder();
            StringBuilder contentOther = new StringBuilder();
            StringBuilder contentTest = new StringBuilder();
            StringBuilder contentWork = new StringBuilder();

            boolean flagContent = false;

            String vid = "";
            String rayName = "";
            String rayNameId = "";

            Map<String, ArrayList<String>> sortMap = new TreeMap<String, ArrayList<String>>(resultAllMap);

            tabs
                    .append("<div class='col' style='padding-left: 0;'>")
                    .append("<div class='nav flex-column nav-pills' style='width: max-content;' role='tablist' aria-orientation ='vertical' id='pills-tab'>");

            contentWork
                    .append("<div class='tab-content' id='pills-tabContentOther'>");


            for (Map.Entry<String, ArrayList<String>> resMapEntry : sortMap.entrySet()) {

                SXObjListParams listParams = new SXObjListParams("sprSubjectFederation");
                listParams.addSelectedAttr("name");
                listParams.addCondition("ouid", resMapEntry.getKey());
                listParams.setGetLinkedObjects(false);
                listParams.getObj();
                SXObj obj = listParams.getObj();
                String regionName = obj.getStringAttr("name");

                if (resMapEntry.getKey().equals(activeRootId)) {
                    tabs
                            .append("<a class='nav-link active' data-toggle='pill' role='tab' style='font-family: Apple Color emoji; padding: .2rem;' aria-selected='true' id='v-");
                    flagContent = true;
                } else if (activeRootId.equals("default")  && resMapEntry.getKey().equals("661")) {
                    tabs
                            .append("<a class='nav-link active' data-toggle='pill' role='tab' style='font-family: Apple Color emoji; padding: .2rem;' aria-selected='true' id='v-");
                    flagContent = true;
                } else {
                    tabs
                            .append("<a class='nav-link' data-toggle='pill' role='tab' style='font-family: Apple Color emoji; padding: .2rem;' aria-selected='false' id='v-");
                }
                tabs
                        .append(resMapEntry.getKey())
                        .append("-tab'")
                        .append(" aria-controls='v-")
                        .append(resMapEntry.getKey())
                        .append("-content'")
                        .append(" href='#v-")
                        .append(resMapEntry.getKey())
                        .append("-content'>")
                        .append(regionName)
                        .append(" ")
                        .append(resMapEntry.getKey())
                        .append("</a>");

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
                    rayNameId = null;
                    for (String currValue : split) {
                        if (vid == null) {
                            vid = currValue;
                        } else if (rayName == null) {
                            rayName = currValue;
                        } else {
                            rayNameId = currValue;
                        }
                    }

                    if (vid.equals("1")) {
                        contentWork
                                .append("<div class='form-check' style='user-select: none;'>")
                                .append("<input class='form-check-input' name='selectAllCheck' type='checkbox' style='width: 30px;' id='")
                                .append(rayNameId)
                                .append("'>")
                                .append("<label class='form-check-label' style='margin-left: 15px; width: 85%;' for='")
                                .append(rayNameId)
                                .append("'>")
                                .append(rayName)
                                .append("</label>")
                                .append("<label style='vertical-align: top;'>")
                                .append(rayNameId)
                                .append("</label>")
                                .append("</div>");

                    } else if (vid.equals("9")) {
                        contentTest
                                .append("<div class='form-check' style='user-select: none;'>")
                                .append("<input class='form-check-input' name='selectAllCheck' type='checkbox' style='width: 30px;' id='")
                                .append(rayNameId)
                                .append("'>")
                                .append("<label class='form-check-label' style='margin-left: 15px; width: 85%;' for='")
                                .append(rayNameId)
                                .append("'>")
                                .append(rayName)
                                .append("</label>")
                                .append("<label style='vertical-align: top;'>")
                                .append(rayNameId)
                                .append("</label>")
                                .append("</div>");
                    } else {
                        contentOther
                                .append("<div class='form-check' style='user-select: none;'>")
                                .append("<input class='form-check-input' name='selectAllCheck' type='checkbox' style='width: 30px;' id='")
                                .append(rayNameId)
                                .append("'>")
                                .append("<label class='form-check-label' style='margin-left: 15px; width: 85%;' for='")
                                .append(rayNameId)
                                .append("'>")
                                .append(rayName)
                                .append("</label>")
                                .append("<label style='vertical-align: top;'>")
                                .append(rayNameId)
                                .append("</label>")
                                .append("</div>");
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

            tabs
                    .append("</div>")
                    .append("</div>");

            contentWork
                    .append("</div>");

            admReq.set("tabsRef", tabs.toString());
            admReq.set("contentRef", contentWork.toString());
        } catch (
                Exception e) {
            e.getStackTrace();
        } finally {
            if (executeQuery != null) {
                executeQuery.close();
            }
        }

        includeTemplate("/WEB-INF/cms/admin/util/registrationpackage/init", admReq);
    }

    public void service(AdmRequest admReq, AdmApplication admApp) throws Exception {
        StringBuilder servicePackageBuilder = new StringBuilder();
        servicePackageBuilder.append("SELECT SS.OUID, P.A_NAME, P.descr, P.OUID AS idPack, SU.DESCRIPTION, REPLACE(CONVERT(VARCHAR, P.TS, 103), '/', '.') AS DateReg  FROM pack P JOIN SPR_SUBJFED SS ON P.region = SS.OUID JOIN SXUSER SU ON SU.OUID = P.CROWNER");
        SXResultSet executeQuery = null;
        SXDb db = SXDsFactory.getDs().getDb();
        String raySelectParams = admReq.getParam("raySelect");
        String[] raySelectItems = raySelectParams.split("-");
        String typeControl = null;
        String raySelectId = null;
        HashMap<String, ArrayList<String>> servicePackageMap = new HashMap<String, ArrayList<String>>();

        for (String select : raySelectItems) {
            if (typeControl == null) {
                typeControl = select;
            } else if (raySelectId == null) {
                raySelectId = select;
            }
        }

        try {
            executeQuery = db.executeQuery(servicePackageBuilder.toString());
            servicePackageBuilder = new StringBuilder();
            while (executeQuery.next()) {
                String regNameOuid = executeQuery.getString("OUID");

                String packageName = executeQuery.getString("A_NAME");
                String descr = executeQuery.getString("descr");
                String id = executeQuery.getString("idPack");
                String crowner = executeQuery.getString("DESCRIPTION");
                String dateReg = executeQuery.getString("DateReg");

                ArrayList<String> get = servicePackageMap.get(regNameOuid);
                if (get != null) {
                    if (!get.contains(regNameOuid)) {
                        get.add(id + ";" + packageName + ";" + descr + ";" + crowner + ";" + dateReg);
                    }
                } else {
                    get = new ArrayList<String>();
                    get.add(id + ";" + packageName + ";" + descr + ";" + crowner + ";" + dateReg);
                }
                servicePackageMap.put(regNameOuid, get);
            }

            String splitPackageId = "";
            String splitPackageName = "";
            String splitDescription = "";
            String splitCrowner = "";
            String splitDateReg = "";

            servicePackageBuilder
                    .append("<div class='col'>");

            for (Map.Entry<String, ArrayList<String>> res : servicePackageMap.entrySet()) {
                for (String resultSplitString : res.getValue()) {
                    String split[] = resultSplitString.split(";");
                    splitPackageId = null;
                    splitPackageName = null;
                    splitDescription = null;
                    splitCrowner = null;
                    splitDateReg = null;

                    for (String currValue : split) {
                        if (splitPackageId == null) {
                            splitPackageId = currValue;
                        } else if (splitPackageName == null) {
                            splitPackageName = currValue;
                        } else if (splitDescription == null) {
                            splitDescription = currValue;
                        } else if (splitCrowner == null) {
                            splitCrowner = currValue;
                        } else {
                            splitDateReg = currValue;
                        }
                    }

                    if (res.getKey().equals(raySelectId)) {
                        servicePackageBuilder
                                .append("<div class='form-check' style='display: grid;'>")
                                .append("<input class='form-check-input' name='selectAllCheck' type='checkbox' style='width: 30px;' id='")
                                .append(splitPackageId)
                                .append("'>")
                                .append("<label class='form-check-label' for='")
                                .append(splitPackageId)
                                .append("'>")
                                .append(splitPackageId)
                                .append(" - ")
                                .append(splitPackageName)
                                .append(" - ")
                                .append(splitDescription)
                                .append(" - ")
                                .append(splitCrowner)
                                .append(" - ")
                                .append(splitDateReg)
                                .append("</label>")
                                .append("</div>");
                    }
                }
            }

            servicePackageBuilder
                    .append("</div>");

            admReq.set("servPackage", servicePackageBuilder.toString());
        } catch (Exception e) {
            e.getCause();
        } finally {
            if (executeQuery != null) {
                executeQuery.close();
            }
        }

        includeTemplate("/WEB-INF/cms/admin/util/registrationpackage/service", admReq);
    }

    public void testSending(AdmRequest admReq, AdmApplication admApp) throws Exception {
        admReq.getDataMap();
    }
}
