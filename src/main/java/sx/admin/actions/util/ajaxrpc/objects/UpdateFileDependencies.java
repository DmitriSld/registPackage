package sx.admin.actions.util.ajaxrpc.objects;

import sx.admin.actions.util.ajaxrpc.AjaxRpcObjectBase;
import sx.datastore.SXDsFactory;
import sx.datastore.db.SXDb;
import sx.datastore.db.SXResultSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class UpdateFileDependencies extends AjaxRpcObjectBase {
    public String service(String reySelect) throws Exception {
        StringBuilder servicePackageBuilder = new StringBuilder();
        servicePackageBuilder.append("SELECT SS.OUID, P.A_NAME, P.descr, P.OUID AS idPack, SU.DESCRIPTION, REPLACE(CONVERT(VARCHAR, P.TS, 103), '/', '.') AS DateReg  FROM pack P JOIN SPR_SUBJFED SS ON P.region = SS.OUID JOIN SXUSER SU ON SU.OUID = P.CROWNER ORDER BY idPack DESC");
        SXResultSet executeQuery = null;
        SXDb db = SXDsFactory.getDs().getDb();
        String[] raySelectItems = reySelect.split("-");
        String typeControl = null;
        String raySelectId = null;
        HashMap<String, ArrayList<String>> servicePackageMap = new HashMap<>();

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
                    get = new ArrayList<>();
                    get.add(id + ";" + packageName + ";" + descr + ";" + crowner + ";" + dateReg);
                }
                servicePackageMap.put(regNameOuid, get);
            }

            String splitPackageId;
            String splitPackageName;
            String splitDescription;
            String splitCrowner;
            String splitDateReg;



            for (Map.Entry<String, ArrayList<String>> res : servicePackageMap.entrySet()) {
                for (String resultSplitString : res.getValue()) {
                    String[] split = resultSplitString.split(";");
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
                        if(resultSplitString.equals(res.getValue().get(0))) {
                            servicePackageBuilder
                                    .append("<div class='row border border-primary rounded border-bottom-0' style='cursor: pointer;' onmouseover='this.style.background=\"#DFEAFF\";' onmouseout='this.style.background=\"white\";' id='row-")
                                    .append(splitPackageId)
                                    .append("' rowPackId='")
                                    .append(splitPackageId)
                                    .append("'>");
                        } else if(resultSplitString.equals(res.getValue().get(res.getValue().size() - 1))) {
                            servicePackageBuilder
                                    .append("<div class='row border border-primary rounded' style='cursor: pointer;' onmouseover='this.style.background=\"#DFEAFF\";' onmouseout='this.style.background=\"white\";' id='row-")
                                    .append(splitPackageId)
                                    .append("' rowPackId='")
                                    .append(splitPackageId)
                                    .append("'>");
                        } else {
                            servicePackageBuilder
                                    .append("<div class='row border border-primary rounded border-bottom-0' style='cursor: pointer;' onmouseover='this.style.background=\"#DFEAFF\";' onmouseout='this.style.background=\"white\";' id='row-")
                                    .append(splitPackageId)
                                    .append("' rowPackId='")
                                    .append(splitPackageId)
                                    .append("'>");
                        }
                        servicePackageBuilder
                                .append("<div class='col-sm-1 border border-primary border-0' style='text-align: center; max-width: 5%; padding: 0; display: flex; justify-content: center; align-items: center; word-break: break-word;'>")
                                .append("<input class='form-check-input-modal' name='modalRowCheck' type='radio' style='width: 30px; margin-top: 0; margin-left: 0;' id='")
                                .append(splitPackageId)
                                .append("-modalinput'>")
                                .append("</div>")
                                .append("<div class='col-sm-1 border border-primary border-right-0 border-top-0 border-bottom-0' onCLick='modalCheckInput(\"row-" + splitPackageId + "\");' style='text-align: center; display: flex; justify-content: center; align-items: center; word-break: break-word;'>")
                                .append(splitPackageId)
                                .append("</div>")
                                .append("<div class='col-sm-3 border border-primary border-right-0 border-top-0 border-bottom-0' onCLick='modalCheckInput(\"row-" + splitPackageId + "\");'  style='display: flex; justify-content: center; align-items: center; word-break: break-word; text-align: center;'>")
                                .append(splitPackageName)
                                .append("</div>")
                                .append("<div class='col border border-primary border-right-0 border-top-0 border-bottom-0' onCLick='modalCheckInput(\"row-" + splitPackageId + "\");' style='text-align: center; display: flex; justify-content: center; align-items: center; word-break: break-word;'>")
                                .append(splitDescription)
                                .append("</div>")
                                .append("<div class='col-sm-2 border border-primary border-right-0 border-top-0 border-bottom-0' onCLick='modalCheckInput(\"row-" + splitPackageId + "\");' style='text-align: center; display: flex; justify-content: center; align-items: center; word-break: break-word;'>")
                                .append(splitCrowner)
                                .append("</div>")
                                .append("<div class='col-sm-2 border border-primary border-right-0 border-top-0 border-bottom-0' onCLick='modalCheckInput(\"row-" + splitPackageId + "\");' style='text-align: center; display: flex; justify-content: center; align-items: center; word-break: break-word; border-right: 1px solid;'>")
                                .append(splitDateReg)
                                .append("</div>")
                                .append("</div>")
                                .append("</div>");
                    }
                }
                servicePackageBuilder
                        .append("</div>");
            }



        } catch (Exception e) {
            e.getCause();
        } finally {
            if (executeQuery != null) {
                executeQuery.close();
            }
        }
        return servicePackageBuilder.toString();
    }
}
