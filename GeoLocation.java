/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.actions;

import com.actionforms.GeoLocationActionForm;
import com.beans.EngineerGeoLocationBean;
import com.exceptions.HDException;
import com.sessioncontainer.CommonSessionContainer;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import com.commons.Constants;
import com.dataaccess.DataAccess;
import com.geoposition.GeoAddressMapApiAction;
import com.mysql.jdbc.exceptions.jdbc4.MySQLSyntaxErrorException;
import com.utils.SDCommonUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletOutputStream;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

/**
 *
 * @author Infoadmin
 */
public class GeoLocation extends BaseAction {

    @Override
    public ActionForward executeAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response, CommonSessionContainer workArea) throws HDException {

        try {
            GeoLocationActionForm l_objForm = (GeoLocationActionForm) form;
            if (workArea == null) {
                workArea.setM_strErrorDetail("Session Expired");
                return mapping.findForward(Constants.FAILURE_FORWARD);
            }
            if (workArea != null) {
                if (workArea.getM_strUserID() == null) {
                    workArea.setM_strErrorDetail("Session Expired");
                    return mapping.findForward(Constants.FAILURE_FORWARD);
                }
            }
            l_objForm.setM_strUserID(workArea.getM_strUserID());
            l_objForm.setM_strUserName(workArea.getM_strUserName());
            l_objForm.setM_strRole(workArea.getM_strRole().trim());
            l_objForm.setM_strMessage("false");
            l_objForm.setM_strMessageType("");
            l_objForm.setM_strMessageforDate("");
            this.fillEngineers(l_objForm, workArea);
            //this.updateEngineerAddressLatLongwise(request); this functionallity added on GetAddress
//            this.updateAddress();
            if (request.getParameter("action") == null) {
                this.Clear(l_objForm);
            }
            if (request.getParameter("action") != null) {
                if (request.getParameter("action").equals("EGeoLocation")) {
//                    this.updateAddress();
                    this.showEngineerGeoLocation(l_objForm);
                    return mapping.findForward(Constants.SUCCESS_FORWARD + "EGeoLocation");
                }
                if (request.getParameter("action").equals("GetAddress")) {
                    this.updateEngineerAddressLatLongwise(request);
                    this.showEngineerGeoLocation(l_objForm);
                    return mapping.findForward(Constants.SUCCESS_FORWARD + "EGeoLocation");
                }
                if (request.getParameter("action").equals("Download")) {
                    this.downloadEngineerGeoLocation(l_objForm, response);
                    return mapping.findForward(Constants.SUCCESS_FORWARD);
                }
                if (request.getParameter("action").equals("checkEngineerLocation")) {
                    try {
                        if (request.getParameter("randomid") != null && l_objForm.getM_strFromDate().equals("") == false && l_objForm.getM_strToDate().equals("") == false) {
                            this.showEngineerGeoLocationOnMap(l_objForm);
                        } else {
                            this.Clear(l_objForm);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

//                    return mapping.findForward(Constants.SUCCESS_FORWARD + "EnggOnMap");
//                    return mapping.findForward(Constants.SUCCESS_FORWARD + "EnggOnMapMultipoint");
                    return mapping.findForward(Constants.SUCCESS_FORWARD + "EnggOnMapMultipoint2");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mapping.findForward(Constants.SUCCESS_FORWARD);
    }

    private void updateAddress() {
        try {
            Connection l_objConnection = DataAccess.connectToDatabase();
            Statement l_objStatement = l_objConnection.createStatement();
            String l_strUpdateAddress = "UPDATE " + Constants.DB_NAME + ".geolocreg_glr g1"
                    + " JOIN " + Constants.DB_NAME + ".geolocreg_glr g2 "
                    + " ON g1.lat_glr = g2.lat_glr and g1.lon_glr=g2.lon_glr and g2.address_glr!='' "
                    + " SET g1.address_glr = g2.address_glr where g1.address_glr=''";
            l_objStatement.executeUpdate(l_strUpdateAddress);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fillEngineers(GeoLocationActionForm l_objForm, CommonSessionContainer workarea) throws SQLException {
        try (Connection l_objConnection = DataAccess.connectToDatabase();
                Statement l_objStatement = l_objConnection.createStatement()) {
            String l_strQuery = "";
            String l_strStateIds = "";
//        if (workarea.getM_strRole().equals("TM") || workarea.getM_strRole().equals("SD")) {
            if (workarea.getM_strRole().equals("SD")) {
                l_strQuery = "select state_sm_em,multilocation_mm_em from " + Constants.DB_NAME + ".engineermst_em where typeid_em='" + workarea.getM_strUserID() + "'";
                try (ResultSet l_objResult = l_objStatement.executeQuery(l_strQuery)) {
                    if (l_objResult != null) {
                        while (l_objResult.next()) {
                            l_strStateIds = l_objResult.getString("state_sm_em");
                            l_strStateIds = l_strStateIds + "," + l_objResult.getString("multilocation_mm_em");
                        }
                    }
                }

                if (l_strStateIds != null && l_strStateIds.endsWith(",")) {
                    l_strStateIds = l_strStateIds.substring(0, l_strStateIds.length() - 1);
                }
                if (SDCommonUtil.checkPopWiseReportingSetting()) {
                    l_strQuery = "SELECT concat(fname_em,' ',lname_em) as name  from " + Constants.DB_NAME + ".engineermst_em"
                            + " where deleteflag_em='N' and role_rm_em IN(2,15) and (emprole_erm_em is null or emprole_erm_em='') ";
                    String l_strWorkAreaPop = workarea.getM_strMultiPopLocations();
                    if (!l_strWorkAreaPop.equals("")) {
                        if (l_strWorkAreaPop.contains(",")) {
                            String[] l_lstPop = l_strWorkAreaPop.split(",");
                            for (int i = 0; i < l_lstPop.length; i++) {
                                if (i == 0) {
                                    l_strQuery += " and (find_in_set('" + l_lstPop[i] + "',concat(poplocation_em,',',COALESCE(multipoplocation_pm_em,''))) ";
                                } else {
                                    l_strQuery += " or find_in_set('" + l_lstPop[i] + "',concat(poplocation_em,',',COALESCE(multipoplocation_pm_em,''))) ";
                                }
                            }
                            l_strQuery += ")";
                        } else {
                            l_strQuery += " and find_in_set('" + l_strWorkAreaPop + "',concat(poplocation_em,',',COALESCE(multipoplocation_pm_em,''))) ";
                        }
                    }
                    l_strQuery += " order by name ";
                } else {
                    l_strQuery = "SELECT concat(fname_em,' ',lname_em) as name  from " + Constants.DB_NAME + ".engineermst_em where deleteflag_em='N' and role_rm_em IN(2,15) and (emprole_erm_em is null or emprole_erm_em='') " + SDCommonUtil.getQueryForBusinessUnit(workarea.getM_strBusinessUnit(), "typeid_bum_em") + " and (state_sm_em in(" + l_strStateIds + ") or multilocation_mm_em in(" + l_strStateIds + ")) order by name ";
                }
                List l_lstEngineers = SDCommonUtil.fillSelectBoxesWithDBValues(l_objStatement, l_strQuery, "name");
                l_lstEngineers.add(0, "Select");
                l_objForm.setM_lstEngineers(l_lstEngineers);

            } //        else if (workarea.getM_strRole().equals("ZM")) {
            //            l_strQuery = "select typeid_sm from " + Constants.DB_NAME + ".statemst_sm where region_rm_sm=" + SDCommonUtil.convertBlankToNull(SDCommonUtil.convertValuesForValueAndIDWithDeleteFlag(l_objStatement, Constants.DB_NAME + ".regionmst_rm", "typevalue_rm", "typeid_rm", SDCommonUtil.convertValuesForValueAndID(l_objStatement, Constants.DB_NAME + ".engineermst_em", "typeid_em", "region_em", "'" + workarea.getM_strUserID() + "'", true), "deleteflag_rm", false), true);
            ////            //System.out.println(l_strQuery);
            //            ResultSet l_objResult = l_objStatement.executeQuery(l_strQuery);
            //            if (l_objResult != null) {
            //                while (l_objResult.next()) {
            //                    l_strStateIds = l_strStateIds + "," + l_objResult.getString("typeid_sm");
            //                }
            //            }
            //            if (l_strStateIds != null && l_strStateIds.endsWith(",")) {
            //                l_strStateIds = l_strStateIds.substring(0, l_strStateIds.length() - 1);
            //            }
            //            l_strQuery = "SELECT concat(fname_em,' ',lname_em) as name  from " + Constants.DB_NAME + ".engineermst_em where deleteflag_em='N' and role_rm_em='2' and (emprole_erm_em is null or emprole_erm_em='') " + SDCommonUtil.getQueryForBusinessUnit(workarea.getM_strBusinessUnit(), "typeid_bum_em") + " and (state_sm_em in(" + l_strStateIds + ") or multilocation_mm_em in(" + l_strStateIds + ")) order by name ";
            //            List l_lstEngineers = SDCommonUtil.fillSelectBoxesWithDBValues(l_objStatement, l_strQuery, "name");
            //            l_lstEngineers.add(0, "Select");
            //            l_objForm.setM_lstEngineers(l_lstEngineers);
            //
            //        }
            else if (workarea.getM_strRole().equals("Technician")) {
                l_strQuery = "SELECT concat(fname_em,' ',lname_em) as name  from " + Constants.DB_NAME + ".engineermst_em where deleteflag_em='N' and resignedflag_em='N' and typeid_em='" + workarea.getM_strUserID() + "'";
                List l_lstEngineers = SDCommonUtil.fillSelectBoxesWithDBValues(l_objStatement, l_strQuery, "name");
                l_objForm.setM_lstEngineers(l_lstEngineers);

            } else {

//            l_strQuery = "SELECT concat(fname_em,' ',lname_em) as name  from " + Constants.DB_NAME + ".engineermst_em where deleteflag_em='N' and role_rm_em='2' and (emprole_erm_em is null or emprole_erm_em='') " + SDCommonUtil.getQueryForBusinessUnit(workarea.getM_strBusinessUnit(), "typeid_bum_em") + " order by name ";
//            l_lstEngineerName = SDCommonUtil.fillSelectBoxesWithDBValues(l_objStatement, l_strQuery, "name");
                List l_lstEngineerName = SDCommonUtil.getEngineerList(workarea.getM_strUserID(), workarea.getM_strRole());
                l_lstEngineerName.add(0, "Select");
                l_objForm.setM_lstEngineers(l_lstEngineerName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void Clear(GeoLocationActionForm l_objForm) {
        l_objForm.setM_strEngineerName("");
        l_objForm.setM_strFromDate("");
        l_objForm.setM_strToDate("");
        l_objForm.setM_lstGeoLocation(new ArrayList<>());
        l_objForm.setM_strTotalPollDistance("");
    }

    private void showEngineerGeoLocation(GeoLocationActionForm l_objForm) throws SQLException {
        try (Connection l_objConnection = DataAccess.connectToDatabase();
                Statement l_objStatement = l_objConnection.createStatement();
                Statement l_objStatement2 = l_objConnection.createStatement()) {
            String l_strQueryGetEID = "", EndDate = "";
            int l_intTypeIDEngineer = 0;
            if (l_objForm.getM_strRole().equals("Technician")) {
                l_intTypeIDEngineer = Integer.parseInt(l_objForm.getM_strUserID());
            } else {
                l_strQueryGetEID = "select typeid_em from " + Constants.DB_NAME + ".engineermst_em where concat(fname_em,' ',lname_em)='" + l_objForm.getM_strEngineerName() + "' "
                        + " AND deleteflag_em='N' and resignedflag_em='N' and role_rm_em IN(2,15) and (emprole_erm_em is null or emprole_erm_em='')";
                try (ResultSet l_objResultSet = l_objStatement.executeQuery(l_strQueryGetEID)) {
                    if (l_objResultSet != null) {
                        while (l_objResultSet.next()) {
                            l_intTypeIDEngineer = l_objResultSet.getInt("typeid_em");
                        }
                    }
                }
            }
            String Startdate = l_objForm.getM_strFromDate();
            EndDate = l_objForm.getM_strToDate();
//        if (Startdate != null && Startdate.equals("") == false) {
//            String l_strQuery = "SELECT DATE_ADD('" + Startdate + "', INTERVAL 1 MONTH)";
//            ResultSet l_objResultSet1 = l_objStatement.executeQuery(l_strQuery);
//            if (l_objResultSet1 != null) {
//                while (l_objResultSet1.next()) {
//                    EndDate = l_objResultSet1.getString(1);
//                }
//            }
//        }
            List l_lstGeoLoc = new ArrayList();
            EngineerGeoLocationBean l_objEngineerGeoLocationBean;
            if (l_intTypeIDEngineer != 0) {

                // Iterating geo loc months table according to start date to current date. - By Pratig Sonar - 04/01/2021
                List<String> l_lstDates = new ArrayList<>();
                l_lstDates = getListMonths(Startdate, EndDate); //get list of months and year between date range.
                //Iteration and execution according to date is done after before query execution.
                // Ends here by Pratig Sonar
                String l_strQuery = "";
                for (String l_strDate : l_lstDates) {
                    try {
                        //by jivaram discuss with SP sir
                        //leave pointer should not display
                        //date:-02-02-2023
                        int l_intTotalCount = 0;
                        l_strQuery = "SELECT *,substring(lastupdated_glr,1,10) as lastupdated  FROM " + Constants.DB_NAME + ".geolocreg_glr" + l_strDate + " where "
                                + "id_em_glr='" + l_intTypeIDEngineer + "' and DATE(lastupdated_glr) >= DATE('" + Startdate + "') and DATE(lastupdated_glr) <= DATE('" + EndDate + "') and updatetype_glr != 'Leave' order by  devicets_glr asc";

                        String l_strCountQuery = "SELECT count(*) FROM " + Constants.DB_NAME + ".geolocreg_glr" + l_strDate + " where "
                                + "id_em_glr='" + l_intTypeIDEngineer + "' and DATE(lastupdated_glr) >= DATE('" + Startdate + "') and DATE(lastupdated_glr) <= DATE('" + EndDate + "') and updatetype_glr != 'Leave' order by  devicets_glr asc";
                        try (ResultSet l_objResultSet = l_objStatement2.executeQuery(l_strCountQuery)) {
                            if (l_objResultSet != null) {
                                while (l_objResultSet.next()) {
                                    l_intTotalCount = l_objResultSet.getInt(1);
                                }
                            }
                        }

                        String l_strDates = "";
                        List l_lstDate = new ArrayList();
                        List l_lstlatlon = new ArrayList();
                        String l_strFromlatlng = "", l_strTolatlng = "", l_strDistance = "", l_strTotalDistance = "";
                        double l_dblDist = 0.00;
                        double l_dblTotalDist = 0.00;
                        int l_intRowCountCurr = 0;
                        int l_intRowCountBefore = 1;
                        double l_intTotalDistanceCount = 0.00;
                        try (ResultSet l_objResultSet = l_objStatement.executeQuery(l_strQuery)) {
                            if (l_objResultSet != null) {
                                while (l_objResultSet.next()) {

                                    l_strTotalDistance = "";
                                    ++l_intRowCountCurr;
                                    ++l_intRowCountBefore;
                                    //curr pos 
                                    int Curr = l_objResultSet.getRow();

                                    l_strDates = SDCommonUtil.convertNullToBlank(l_objResultSet.getString("lastupdated"), false);
                                    if (l_lstDate.contains(l_strDates)) {

                                        l_dblTotalDist = l_dblTotalDist + l_dblDist;
//                                l_dblTotalDist = Math.round(l_dblTotalDist);
                                        l_dblTotalDist = Double.parseDouble(DF.format(l_dblTotalDist));

                                        int lastindex = l_lstlatlon.size() - 1;
                                        l_strFromlatlng = l_lstlatlon.get(lastindex).toString();

                                        l_strTolatlng = SDCommonUtil.convertNullToBlank(l_objResultSet.getString("lat_glr"), false) + "," + SDCommonUtil.convertNullToBlank(l_objResultSet.getString("lon_glr"), false);

                                    }
                                    l_lstDate.add(l_strDates);

                                    String l_strlatlon = SDCommonUtil.convertNullToBlank(l_objResultSet.getString("lat_glr"), false) + "," + SDCommonUtil.convertNullToBlank(l_objResultSet.getString("lon_glr"), false);
                                    l_lstlatlon.add(l_strlatlon);

                                    String l_strLatlng = SDCommonUtil.convertNullToBlank(l_objResultSet.getString("provider_glr"), false) + "," + SDCommonUtil.convertNullToBlank(l_objResultSet.getString("lon_glr"), false);

                                    l_objEngineerGeoLocationBean = new EngineerGeoLocationBean();
                                    l_objEngineerGeoLocationBean.setM_strLatitude(SDCommonUtil.convertNullToBlank(l_objResultSet.getString("lat_glr"), false));
                                    l_objEngineerGeoLocationBean.setM_strLongitude(SDCommonUtil.convertNullToBlank(l_objResultSet.getString("lon_glr"), false));
                                    l_objEngineerGeoLocationBean.setM_strLastUpdate(SDCommonUtil.convertNullToBlank(l_objResultSet.getString("lastupdated_glr"), false));
                                    l_objEngineerGeoLocationBean.setM_strDeviceDateTime(SDCommonUtil.convertNullToBlank(l_objResultSet.getString("devicets_glr"), false));
                                    l_objEngineerGeoLocationBean.setM_strLoginIDGLR(SDCommonUtil.convertNullToBlank(l_objResultSet.getString("logid_glr"), false));
                                    l_objEngineerGeoLocationBean.setM_strAddress(SDCommonUtil.convertNullToBlank(l_objResultSet.getString("address_glr"), false));
                                    l_objEngineerGeoLocationBean.setM_strUpdateType(SDCommonUtil.convertNullToBlank(l_objResultSet.getString("updatetype_glr"), false));
                                    l_objEngineerGeoLocationBean.setM_strTicketID(SDCommonUtil.convertNullToBlank(l_objResultSet.getString("ticketid_glr"), false));

                                    //Distance Calculation From two lat lng
                                    if (!l_strFromlatlng.equals("") && !l_strTolatlng.equals("")) {

                                        l_dblDist = getDiffOfLatLon(l_strFromlatlng, l_strTolatlng);
                                        l_dblDist = l_dblDist / 1000;
                                        double l_strDistance2 = (l_dblDist * 25) / 100;
                                        l_dblDist = l_dblDist + l_strDistance2;
//                                l_dblDist = Math.round(l_dblDist);
                                        l_dblDist = Double.parseDouble(DF.format(l_dblDist));

                                        if (SDCommonUtil.convertNullToBlank(l_objResultSet.getString("updatetype_glr"), false).equalsIgnoreCase("login")) {
                                            l_dblDist = 0.00;
                                            l_dblTotalDist = 0.00;
                                        }
//                                        if(SDCommonUtil.convertNullToBlank(l_objResultSet.getString("updatetype_glr"),false).equalsIgnoreCase("ETA")){
//                                            l_dblDist = 0.00;                                                    
//                                        }

                                    }
                                    if (l_intRowCountBefore <= l_intTotalCount) {
                                        l_objResultSet.absolute(l_intRowCountBefore);
                                        //before pos 
                                        Curr = l_objResultSet.getRow();
                                    }

                                    if (!l_strDates.equals(SDCommonUtil.convertNullToBlank(l_objResultSet.getString("lastupdated"), false))) {
                                        l_dblTotalDist = l_dblTotalDist + l_dblDist;
//                                l_dblTotalDist = Math.round(l_dblTotalDist);
                                        l_dblTotalDist = Double.parseDouble(DF.format(l_dblTotalDist));

                                        if (l_dblTotalDist == 0.00) {

                                            l_intTotalDistanceCount = l_intTotalDistanceCount + l_dblTotalDist;
                                            l_intTotalDistanceCount = Double.parseDouble(DF.format(l_intTotalDistanceCount));
                                            l_strTotalDistance = "0 Km";
                                            l_dblTotalDist = 0.00;
                                        } else {

                                            l_intTotalDistanceCount = l_intTotalDistanceCount + l_dblTotalDist;
                                            l_intTotalDistanceCount = Double.parseDouble(DF.format(l_intTotalDistanceCount));
                                            l_strTotalDistance = l_dblTotalDist + " Km";
                                            l_dblTotalDist = 0.00;
                                        }
//                                            ++l_intCount;
//                                            l_dblTotalDist = 0.0;
                                    }

                                    l_objResultSet.absolute(l_intRowCountCurr);
                                    //curr pos 
                                    Curr = l_objResultSet.getRow();

                                    if (Curr == l_intTotalCount) {
                                        l_dblTotalDist = l_dblTotalDist + l_dblDist;
//                                l_dblTotalDist = Math.round(l_dblTotalDist);
                                        l_dblTotalDist = Double.parseDouble(DF.format(l_dblTotalDist));
//                                            if(l_intCount == 0){
                                        if (l_dblTotalDist == 0.00) {

                                            l_intTotalDistanceCount = l_intTotalDistanceCount + l_dblTotalDist;
                                            l_intTotalDistanceCount = Double.parseDouble(DF.format(l_intTotalDistanceCount));
                                            l_strTotalDistance = "0 Km";
                                            l_dblTotalDist = 0.00;
                                        } else {
//                                    if(l_dblTotalDist < 1.0){
//                                        l_intTotalDistanceCount = l_intTotalDistanceCount + l_dblTotalDist;
//                                        l_strTotalDistance = l_dblTotalDist + " m";
//                                        l_dblTotalDist = 0.0;
//                                    } else{

                                            l_intTotalDistanceCount = l_intTotalDistanceCount + l_dblTotalDist;
                                            l_intTotalDistanceCount = Double.parseDouble(DF.format(l_intTotalDistanceCount));
                                            l_strTotalDistance = l_dblTotalDist + " Km";
                                            l_dblTotalDist = 0.00;
//                                    }
                                        }
//                                            }
                                    }
                                    if (l_dblDist == 0.00) {
                                        l_strDistance = "0 Km";
                                    } else {
//                                if(l_dblDist < 1.0){
//                                    l_strDistance = l_dblDist + " m";
//                                } else{
                                        l_strDistance = l_dblDist + " Km";
//                                }
                                    }
                                    
//                                    if(SDCommonUtil.convertNullToBlank(l_objResultSet.getString("updatetype_glr"),false).equalsIgnoreCase("ETA")){
//                                        l_strDistance = "0 Km";
//                                    }

                                    l_objEngineerGeoLocationBean.setM_strPollToPollDistance(SDCommonUtil.convertNullToBlank(l_strDistance, false));
                                    l_objEngineerGeoLocationBean.setM_strTotalPollToPollDistance(SDCommonUtil.convertNullToBlank(l_strTotalDistance, false));

                                    l_objEngineerGeoLocationBean.setM_strAccuracy(SDCommonUtil.convertNullToBlank(l_objResultSet.getString("accuracy_glr"), false));
                                    l_objEngineerGeoLocationBean.setM_strAltitude(SDCommonUtil.convertNullToBlank(l_objResultSet.getString("altitude_glr"), false));
                                    l_objEngineerGeoLocationBean.setM_strElapsedTime(SDCommonUtil.convertNullToBlank(l_objResultSet.getString("elapsedtime_glr"), false));
                                    l_objEngineerGeoLocationBean.setM_strProvider(SDCommonUtil.convertNullToBlank(l_objResultSet.getString("provider_glr"), false));
                                    l_objEngineerGeoLocationBean.setM_strLatLon(l_strLatlng);
                                    l_lstGeoLoc.add(l_objEngineerGeoLocationBean);
                                    l_objEngineerGeoLocationBean = null;
                                    l_objForm.setM_strMessage("RecordExist");
                                }
                            }
                        }
                        l_intTotalDistanceCount = Double.parseDouble(DF.format(l_intTotalDistanceCount));
                        String l_strTotalDistanceCount = "";
                        if (l_intTotalDistanceCount == 0.00) {
                            l_strTotalDistanceCount = "0 Km";
                        } else {
                            l_strTotalDistanceCount = l_intTotalDistanceCount + " Km";
                        }
                        l_objForm.setM_strTotalPollDistance(l_strTotalDistanceCount);

                    } catch (MySQLSyntaxErrorException ex) {
                        if (ex.getErrorCode() == 1146) {
                            //Table doesn't exist. 
                            //We checking if table exists and if not we are continuing with the loop for other tables.
                        }
                    }
                }
                l_objForm.setM_lstGeoLocation(l_lstGeoLoc);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showEngineerGeoLocationOnMap(GeoLocationActionForm l_objForm) {
        try (Connection l_objConnection = DataAccess.connectToDatabase();
                Statement l_objStatement = l_objConnection.createStatement();) {
            if (l_objForm.getM_strFromDate().contains("/")) {
                String[] l_strFromDateFormat = l_objForm.getM_strFromDate().split("/");
                l_objForm.setM_strFromDate(l_strFromDateFormat[2] + "-" + l_strFromDateFormat[0] + "-" + l_strFromDateFormat[1]);
            }
            if (l_objForm.getM_strToDate().contains("/")) {
                String[] l_strToDateFormat = l_objForm.getM_strToDate().split("/");
                l_objForm.setM_strToDate(l_strToDateFormat[2] + "-" + l_strToDateFormat[0] + "-" + l_strToDateFormat[1]);
            }
            if (l_objForm.getM_strFromDate() != null && l_objForm.getM_strFromDate().equals("") == false
                    && l_objForm.getM_strToDate() != null && l_objForm.getM_strToDate().equals("") == false
                    && l_objForm.getM_strEngineerName() != null && l_objForm.getM_strEngineerName().equals("") == false && l_objForm.getM_strEngineerName().equals("Select") == false) {
                String l_strQueryGetEID = "";
                int l_intTypeIDEngineer = 0;
                String l_strEnggStateId = "";
                l_strQueryGetEID = "select typeid_em,state_sm_em from " + Constants.DB_NAME + ".engineermst_em where concat(fname_em,' ',lname_em)='" + l_objForm.getM_strEngineerName() + "' and deleteflag_em='N' and resignedflag_em='N' and role_rm_em IN (2,15) and (emprole_erm_em is null or emprole_erm_em='')";
                try (ResultSet l_objResultSet = l_objStatement.executeQuery(l_strQueryGetEID)) {
                    if (l_objResultSet != null) {
                        while (l_objResultSet.next()) {
                            l_intTypeIDEngineer = l_objResultSet.getInt("typeid_em");
                            l_strEnggStateId = l_objResultSet.getString("state_sm_em");
                        }
                    }
                }
                EngineerGeoLocationBean l_objEngineerGeoLocationBean;

                String l_strStateName = SDCommonUtil.convertValuesForValueAndIDWithDeleteFlag(l_objStatement, Constants.DB_NAME + ".statemst_sm", "typeid_sm", "typevalue_sm", "'" + l_strEnggStateId + "'", "deleteflag_sm", false);
                if (l_strStateName != null && l_strStateName.equals("") == false) {
                    GeoAddressMapApiAction l_objGeoAddressMapApiAction = new GeoAddressMapApiAction();
                    if (l_objGeoAddressMapApiAction.getGPSSetting("ShowRoute") == 1) {
                        l_objForm.setM_strMapCenterLatLon(l_objGeoAddressMapApiAction.getLatLonFromAddress(l_strStateName));
                    } else {
                        l_objForm.setM_strMapCenterLatLon(",");
                    }

                } else {
                    l_objForm.setM_strMapCenterLatLon("20.5937,78.9629");
                }

                if (l_objForm.getM_strMapCenterLatLon() == null || l_objForm.getM_strMapCenterLatLon().equals("") || l_objForm.getM_strMapCenterLatLon().equals(",")) {
                    l_objForm.setM_strMapCenterLatLon("20.5937,78.9629");
                }

                List l_lstList = new ArrayList();
                int l_strDateDiff = 0;
                String l_strDateQuery = "select datediff('" + l_objForm.getM_strToDate() + "','" + l_objForm.getM_strFromDate() + "') as date_difference";
//                String l_strDateQuery = "select datediff('2019-04-09', '2019-04-03') as date_difference";
                try (ResultSet l_objResultSet1 = l_objStatement.executeQuery(l_strDateQuery)) {
                    if (l_objResultSet1 != null) {
                        while (l_objResultSet1.next()) {
                            l_strDateDiff = l_objResultSet1.getInt(1);
                            if (l_strDateDiff <= 2) {

                            } else {
//                    String l_strMsg;
                                l_objForm.setM_strMessageforDate("Date should be  equal to 2 ");
//                    l_strMsg("Date should be  equal to 2 ");
                            }

                        }
                    }
                }
                String l_strConcatLatLon = "";
                String strLatA = "";
                String strLonA = "";
                String l_strFromLatLog = "0.0,0.0";
                String l_strToLatLog = "0.0,0.0";
                double distanceTraveled = 0.0f;
                double distanceGetFromLatLog = 0.0;
                String l_strQuery = "select * from " + Constants.DB_NAME + ".geolocreg_glr where id_em_glr='" + l_intTypeIDEngineer + "' and date(devicets_glr) between '" + l_objForm.getM_strFromDate() + "' and '" + l_objForm.getM_strToDate() + "' and lat_glr is not null and lat_glr!='' and lon_glr is not null and lon_glr!=''";
//                String l_strQuery = "select * from " + Constants.DB_NAME + ".geolocreg_glr where id_em_glr='601' and date(devicets_glr) between '2019-04-03' and '2019-04-04' and lat_glr is not null and lat_glr!='' and lon_glr is not null and lon_glr!=''";
                try (ResultSet l_objResultSet = l_objStatement.executeQuery(l_strQuery)) {
                    if (l_objResultSet != null) {
                        while (l_objResultSet.next()) {
                            l_objEngineerGeoLocationBean = new EngineerGeoLocationBean();
//                        if (l_objResultSet.isFirst()) {
//                            l_objForm.setM_strStartPoint(l_objResultSet.getString("lat_glr")+ "," + l_objResultSet.getString("lon_glr"));
//                        }
//                        if (l_objResultSet.isLast()) {
//                            l_objForm.setM_strEndPoint(l_objResultSet.getString("lat_glr")+ "," + l_objResultSet.getString("lon_glr"));
//                        }
//                        if (l_objResultSet.isFirst()) {
//                            l_strConcatLatLon = "{'Geometry':{'Latitude':" + l_objResultSet.getString("lat_glr") + ",'Longitude':" + l_objResultSet.getString("lon_glr") + ",'Details':" + l_objResultSet.getString("devicets_glr") + "}}";
//                        } else {
//                            l_strConcatLatLon = l_strConcatLatLon + "," + "{'Geometry':{'Latitude':" + l_objResultSet.getString("lat_glr") + ",'Longitude':" + l_objResultSet.getString("lon_glr") + ",'Details':" + l_objResultSet.getString("devicets_glr") + "}}";
//                        }

                            if (l_objResultSet.isFirst()) {
                                strLatA = l_objResultSet.getString("lat_glr");
                                strLonA = l_objResultSet.getString("lon_glr");
                                l_strFromLatLog = strLatA + "," + strLonA;
                                if (l_objResultSet.getString("ticketid_glr") != null) {
                                    l_objEngineerGeoLocationBean.setM_strTotalDistanceTraveledByEngineer(l_objResultSet.getString("updatetype_glr") + " Ticket Id :" + l_objResultSet.getString("ticketid_glr") + " Distance :" + String.format("%.2f", distanceTraveled) + "Km ");
                                } else {
                                    l_objEngineerGeoLocationBean.setM_strTotalDistanceTraveledByEngineer(l_objResultSet.getString("updatetype_glr") + " Distance :" + String.format("%.2f", distanceTraveled) + "Km");
                                }
                                l_objEngineerGeoLocationBean.setM_strLat(l_objResultSet.getString("lat_glr"));
                                l_objEngineerGeoLocationBean.setM_strLon(l_objResultSet.getString("lon_glr"));
                                l_objEngineerGeoLocationBean.setM_strAddress(SDCommonUtil.checkEscapeSequenceCharacter(l_objResultSet.getString("address_glr")));
                                l_objEngineerGeoLocationBean.setM_strDeviceDateTime(l_objResultSet.getString("devicets_glr"));
                                l_lstList.add(l_objEngineerGeoLocationBean);
                                l_objEngineerGeoLocationBean = null;
                            } //if (strLatA.equals(l_objResultSet.getString("lat_glr")) && strLonA.equals(l_objResultSet.getString("lon_glr"))) {
                            //Do Nothing
                            //}
                            else {
                                l_objEngineerGeoLocationBean.setM_strLat(l_objResultSet.getString("lat_glr"));
                                l_objEngineerGeoLocationBean.setM_strLon(l_objResultSet.getString("lon_glr"));
                                l_strToLatLog = l_objEngineerGeoLocationBean.getM_strLat() + "," + l_objEngineerGeoLocationBean.getM_strLon();
                                l_objEngineerGeoLocationBean.setM_strAddress(SDCommonUtil.checkEscapeSequenceCharacter(l_objResultSet.getString("address_glr")));
                                l_objEngineerGeoLocationBean.setM_strDeviceDateTime(l_objResultSet.getString("devicets_glr"));
                                System.out.println("pri  " + distanceTraveled);
                                distanceGetFromLatLog = getDiffOfLatLon(l_strFromLatLog, l_strToLatLog);
                                distanceGetFromLatLog = distanceGetFromLatLog / 1000;
                                double l_strDistance2 = (distanceGetFromLatLog * 25) / 100;
                                distanceGetFromLatLog = distanceGetFromLatLog + l_strDistance2;
                                System.out.println("dis  " + distanceGetFromLatLog);
                                distanceTraveled = distanceTraveled + distanceGetFromLatLog;
//                            System.out.println("total "+ distanceTraveled);
                                if (l_objResultSet.getString("ticketid_glr") != null) {
                                    l_objEngineerGeoLocationBean.setM_strTotalDistanceTraveledByEngineer(l_objResultSet.getString("updatetype_glr") + " Ticket Id :" + l_objResultSet.getString("ticketid_glr") + " Distance :" + String.format("%.2f", distanceTraveled) + "Km   ");
                                } else {
                                    l_objEngineerGeoLocationBean.setM_strTotalDistanceTraveledByEngineer(l_objResultSet.getString("updatetype_glr") + " Distance :" + String.format("%.2f", distanceTraveled) + "Km");
                                }

                                strLatA = l_objResultSet.getString("lat_glr");
                                strLonA = l_objResultSet.getString("lon_glr");
                                boolean isLastValueNotZero = true;
                                if (strLatA != null && strLonA != null) {
                                    if (strLatA.contains(".") && strLonA.contains(".")) {
                                        float lat1 = Float.parseFloat(strLatA);
                                        float lng1 = Float.parseFloat(strLonA);
                                        if (lat1 == 0 && lng1 == 0) {
                                            isLastValueNotZero = false;
                                        }
                                    }
                                }
                                if (isLastValueNotZero) {
                                    l_lstList.add(l_objEngineerGeoLocationBean);
                                    l_objEngineerGeoLocationBean = null;
                                    l_strFromLatLog = strLatA + "," + strLonA;
                                }
                            }
                        }
                    }
                }
//                l_objForm.setM_strConcatLatLon(l_strConcatLatLon);
                l_objForm.setM_lstGeoLocation(l_lstList);
//                l_objForm.setM_strStartPoint("20.5937,78.9629");
//                l_objResultSet.close();
//                l_objStatement.close();
//                l_objConnection.close();

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateEngineerAddressLatLongwise(HttpServletRequest request) {
        String l_strAddressFromLatLon = "";
        try (Connection l_objConnection = DataAccess.connectToDatabase();
                Statement l_objStatement = l_objConnection.createStatement();) {
            String l_strLatLong = SDCommonUtil.convertNullToBlank(request.getParameter("latLong"), false);

            if (!l_strLatLong.equals("") && !l_strLatLong.equals("0,0")) {
                GeoAddressMapApiAction l_objGeoAddressMapApiAction = new GeoAddressMapApiAction();
                if (l_objGeoAddressMapApiAction.getGPSSetting("GeoLocation") == 1) {
                    l_strAddressFromLatLon = l_objGeoAddressMapApiAction.getAddressFromLatLon(l_strLatLong);
                }
                if (!l_strAddressFromLatLon.equals("")) {
                    String l_strQuery = "update " + Constants.DB_NAME + ".geolocreg_glr set address_glr='" + l_strAddressFromLatLon + "' where concat(lat_glr,',',lon_glr)='" + l_strLatLong + "' and (address_glr='' or address_glr is null)";
                    l_objStatement.executeUpdate(l_strQuery);
                    String l_strQuery1 = "update " + Constants.DB_NAME + ".engavailabilitymst_eam set address_eam='" + l_strAddressFromLatLon + "' where concat(lat_eam,',',lon_eam)='" + l_strLatLong + "' and (address_eam='' or address_eam is null)";
                    l_objStatement.executeUpdate(l_strQuery1);
                    System.out.println("GeoLocationAction:" + l_strQuery);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    //commented by jivaram INF ID:-3982
    //date:-04-03-2023
//    private void downloadEngineerGeoLocation(GeoLocationActionForm l_objForm, HttpServletResponse response) {
//        try (Connection l_objConnection = DataAccess.connectToDatabase();
//                Statement l_objStatement = l_objConnection.createStatement();
//                Statement l_objStatement2 = l_objConnection.createStatement()) {
//
//            String l_strQueryGetEID = "", EndDate = "";
//            int l_intTypeIDEngineer = 0;
//
//            if (l_objForm.getM_strRole().equals("Technician")) {
//                l_intTypeIDEngineer = Integer.parseInt(l_objForm.getM_strUserID());
//            } else {
//                l_strQueryGetEID = "select typeid_em from " + Constants.DB_NAME + ".engineermst_em where concat(fname_em,' ',lname_em)='" + l_objForm.getM_strEngineerName() + "' "
//                        + " AND deleteflag_em='N' and resignedflag_em='N' and role_rm_em IN (2,15) and (emprole_erm_em is null or emprole_erm_em='')";
//                try (ResultSet l_objResultSet = l_objStatement.executeQuery(l_strQueryGetEID)) {
//                    if (l_objResultSet != null) {
//                        while (l_objResultSet.next()) {
//                            l_intTypeIDEngineer = l_objResultSet.getInt("typeid_em");
//                        }
//                    }
//                }
//            }
//            String Startdate = l_objForm.getM_strFromDate();
////            if (Startdate != null && Startdate.equals("") == false) {
////                String l_strQuery = "SELECT DATE_ADD('" + Startdate + "', INTERVAL 1 MONTH)";
////                ResultSet l_objResultSet1 = l_objStatement.executeQuery(l_strQuery);
////                if (l_objResultSet1 != null) {
////                    while (l_objResultSet1.next()) {
////                        EndDate = l_objResultSet1.getString(1);
////                    }
////                }
////            }
//            EndDate = l_objForm.getM_strToDate();
//            boolean l_blnQuotes = true;
//            ArrayList l_lstColumnHeader = new ArrayList();
//
//            l_lstColumnHeader.add(0, "Last Updated Date");
//            l_lstColumnHeader.add(1, "Devicets");
//            l_lstColumnHeader.add(2, "Lat");
//            l_lstColumnHeader.add(3, "Lon");
//            l_lstColumnHeader.add(4, "Address");
//            l_lstColumnHeader.add(5, "Pointers");
//            l_lstColumnHeader.add(6, "Concern Ticket ID");
//            l_lstColumnHeader.add(7, "Distance Travelled Poll to Poll");
//            l_lstColumnHeader.add(8, "Total Distance Travelled (PD)");
//            l_lstColumnHeader.add(9, "Accuracy");
//            l_lstColumnHeader.add(10, "Altitude");
//            l_lstColumnHeader.add(11, "Elapsed Time");
//            l_lstColumnHeader.add(12, "Provider");
//
//            if (l_intTypeIDEngineer != 0) {
//                // Create workbook
//                String filedirectory = System.getProperty("catalina.base");
//                //commented by jivaram INF ID:- 3872
//                //date:-30-01-2023
////                filedirectory = filedirectory + "/webapps/EngineerLocationReport/";
//                File dir = new File(filedirectory + "/webapps/EngineerLocationReport/");
//                if (dir.isDirectory() == false) {
//                    dir.mkdir();
//                }
//                // Work book create
//                Workbook workbook = new SXSSFWorkbook();
//
//                FileOutputStream out = new FileOutputStream(new File(filedirectory + "_" + "EngineerLocationReport" + ".xlsx"));
//
////            FileOutputStream out = new FileOutputStream(new File(filedirectory + "CustomerActivity.xlsx"));
//                SXSSFSheet sheet = (SXSSFSheet) workbook.createSheet("EngineerLocationReport_Reports");
//
//                // set Font for column for 1st sheet
//                Font font = workbook.createFont();
//                font.setColor(IndexedColors.WHITE.getIndex());
//                font.setFontHeightInPoints((short) (10.5));
//                font.setFontName("Zurich BT");
//                font.setBoldweight((short) 25);
//
//                CellStyle cellStyle1 = null;
//                cellStyle1 = workbook.createCellStyle();
//                cellStyle1.setBorderBottom(CellStyle.BORDER_THIN);
//                cellStyle1.setBorderLeft(CellStyle.BORDER_THIN);
//                cellStyle1.setBorderRight(CellStyle.BORDER_THIN);
//                cellStyle1.setBorderTop(CellStyle.BORDER_THIN);
//                cellStyle1.setBottomBorderColor(IndexedColors.BLACK.getIndex());
//                cellStyle1.setLeftBorderColor(IndexedColors.BLACK.getIndex());
//                cellStyle1.setRightBorderColor(IndexedColors.BLACK.getIndex());
//                cellStyle1.setTopBorderColor(IndexedColors.BLACK.getIndex());
//                cellStyle1.setAlignment(cellStyle1.ALIGN_CENTER);
//                cellStyle1.setFillForegroundColor(IndexedColors.BLACK.getIndex());
//                cellStyle1.setFillPattern(CellStyle.SOLID_FOREGROUND);
//                cellStyle1.setFillPattern(CellStyle.SOLID_FOREGROUND);
//                cellStyle1.setFont(font);
//
//                long rowCount = 1;
//                int SheetCount = 0;
//                Row row = sheet.createRow((short) 0);
//                Cell cell = row.createCell((short) 0);
//                Row rowHeader = sheet.createRow((short) 0);
//                for (int K = 0; K < l_lstColumnHeader.size(); K++) {
//                    cell = rowHeader.createCell((short) K);
//                    cell.setCellValue(l_lstColumnHeader.get(K).toString());
//                    cell.setCellStyle(cellStyle1);
//                    sheet.autoSizeColumn(K);
//
//                }
//
//                // Iterating geo loc months table according to start date to current date. - By Pratig Sonar - 04/01/2021
//                List<String> l_lstDates = new ArrayList<>();
//                l_lstDates = getListMonths(Startdate, EndDate); //get list of months and year between date range.
//                //Iteration and execution according to date is done after before query execution.
//                // Ends here by Pratig Sonar
//                String l_strQuery = "";
//                for (String l_strDate : l_lstDates) {
//                    try {
//                        //by jivaram discuss with SP sir
//                        //leave pointer should not display
//                        //date:-02-02-2023
//                        int l_intTotalCount = 0;
//                        l_strQuery = "SELECT *,substring(lastupdated_glr,1,10) as lastupdated FROM " + Constants.DB_NAME + ".geolocreg_glr" + l_strDate + " where "
//                                + "id_em_glr='" + l_intTypeIDEngineer + "' and DATE(lastupdated_glr) >= DATE('" + Startdate + "') and DATE(lastupdated_glr) <= DATE('" + EndDate + "') and updatetype_glr != 'Leave' order by  devicets_glr asc";
////                        l_strQuery = "SELECT * FROM " + Constants.DB_NAME + ".geolocreg_glr" + l_strDate + " where "
////                                + "id_em_glr='" + l_intTypeIDEngineer + "' and DATE(lastupdated_glr) >= DATE('" + Startdate + "') and DATE(lastupdated_glr) <= DATE('" + EndDate + "') order by  devicets_glr desc";
//
//                        String l_strCountQuery = "SELECT count(*) FROM " + Constants.DB_NAME + ".geolocreg_glr" + l_strDate + " where "
//                                + "id_em_glr='" + l_intTypeIDEngineer + "' and DATE(lastupdated_glr) >= DATE('" + Startdate + "') and DATE(lastupdated_glr) <= DATE('" + EndDate + "') and updatetype_glr != 'Leave' order by  devicets_glr asc";
//                        try (ResultSet l_objResultSet = l_objStatement2.executeQuery(l_strCountQuery)) {
//                            if (l_objResultSet != null) {
//                                while (l_objResultSet.next()) {
//                                    l_intTotalCount = l_objResultSet.getInt(1);
//                                }
//                            }
//                        }
//                        rowCount = l_intTotalCount;
//                        SheetCount = SheetCount + l_intTotalCount;
//                        if(SheetCount > 0){
//                            sheet.setRandomAccessWindowSize(SheetCount);
//                        }
//                        String l_strDates = "";
//                        List l_lstDate = new ArrayList();
//                        List l_lstlatlon = new ArrayList();
//                        String l_strFromlatlng = "", l_strTolatlng = "", l_strDistance = "", l_strTotalDistance = "";
//                        double l_dblDist = 0.00;
//                        double l_dblTotalDist = 0.00;
//                        int l_intRowCountCurr = 0;
//                        int l_intRowCountBefore = 1;
//                        int l_intCount = 0;
//                        try (ResultSet l_objResultSet = l_objStatement.executeQuery(l_strQuery)) {
//                            if (l_objResultSet != null) {
//
////                            l_objResultSet.afterLast();
////                            while (l_objResultSet.previous()) {
//                                while (l_objResultSet.next()) {
//                                    l_strTotalDistance = "";
//                                    ++l_intRowCountCurr;
//                                    ++l_intRowCountBefore;
//
//                                    //curr pos 
//                                    int Curr = l_objResultSet.getRow();
//
//                                    l_strDates = SDCommonUtil.convertNullToBlank(l_objResultSet.getString("lastupdated"), false);
//                                    if (l_lstDate.contains(l_strDates)) {
//
//                                        l_dblTotalDist = l_dblTotalDist + l_dblDist;
////                                    l_dblTotalDist = Math.round(l_dblTotalDist);
//                                        l_dblTotalDist = Double.parseDouble(DF.format(l_dblTotalDist));
//
//                                        int lastindex = l_lstlatlon.size() - 1;
//                                        l_strFromlatlng = l_lstlatlon.get(lastindex).toString();
//
//                                        l_strTolatlng = SDCommonUtil.convertNullToBlank(l_objResultSet.getString("lat_glr"), false) + "," + SDCommonUtil.convertNullToBlank(l_objResultSet.getString("lon_glr"), false);
//
//                                    }
//                                    l_lstDate.add(l_strDates);
//
//                                    String l_strlatlon = SDCommonUtil.convertNullToBlank(l_objResultSet.getString("lat_glr"), false) + "," + SDCommonUtil.convertNullToBlank(l_objResultSet.getString("lon_glr"), false);
//                                    l_lstlatlon.add(l_strlatlon);
//
//                                    Row RowValue = sheet.createRow((int) (long) rowCount);
//                                    Cell cellA1 = RowValue.createCell((short) 0);
//                                    Cell cellB1 = RowValue.createCell((short) 1);
//                                    Cell cellC1 = RowValue.createCell((short) 2);
//                                    Cell cellD1 = RowValue.createCell((short) 3);
//                                    Cell cellE1 = RowValue.createCell((short) 4);
//                                    Cell cellR1 = RowValue.createCell((short) 5);
//                                    Cell cellP1 = RowValue.createCell((short) 6);
//                                    Cell cellF1 = RowValue.createCell((short) 7);
//                                    Cell cellG1 = RowValue.createCell((short) 8);
//                                    Cell cellH1 = RowValue.createCell((short) 9);
//                                    Cell cellI1 = RowValue.createCell((short) 10);
//                                    Cell cellJ1 = RowValue.createCell((short) 11);
//                                    Cell cellK1 = RowValue.createCell((short) 12);
//
//                                    cellA1.setCellValue(SDCommonUtil.convertNullToBlank(l_objResultSet.getString("lastupdated_glr"), !l_blnQuotes));
//                                    cellB1.setCellValue(SDCommonUtil.convertNullToBlank(l_objResultSet.getString("devicets_glr"), !l_blnQuotes));
//                                    cellC1.setCellValue(SDCommonUtil.convertNullToBlank(l_objResultSet.getString("lat_glr"), !l_blnQuotes));
//                                    cellD1.setCellValue(SDCommonUtil.convertNullToBlank(l_objResultSet.getString("lon_glr"), !l_blnQuotes));
//                                    cellE1.setCellValue(SDCommonUtil.convertNullToBlank(l_objResultSet.getString("address_glr"), !l_blnQuotes));
//                                    cellR1.setCellValue(SDCommonUtil.convertNullToBlank(l_objResultSet.getString("updatetype_glr"), !l_blnQuotes));
//                                    cellP1.setCellValue(SDCommonUtil.convertNullToBlank(l_objResultSet.getString("ticketid_glr"), !l_blnQuotes));
//
//                                    //Distance Calculation From two lat lng
//                                    if (!l_strFromlatlng.equals("") && !l_strTolatlng.equals("")) {
//
//                                        l_dblDist = getDiffOfLatLon(l_strFromlatlng, l_strTolatlng);
//                                        l_dblDist = l_dblDist / 1000;
//                                        double l_strDistance2 = (l_dblDist * 25) / 100;
//                                        l_dblDist = l_dblDist + l_strDistance2;
////                                    l_dblDist = Math.round(l_dblDist);
//                                        l_dblDist = Double.parseDouble(DF.format(l_dblDist));
//
//                                        if (SDCommonUtil.convertNullToBlank(l_objResultSet.getString("updatetype_glr"), false).equalsIgnoreCase("login")) {
//                                            l_dblDist = 0.00;
//                                            l_dblTotalDist = 0.00;
//                                        }
////                                        if(SDCommonUtil.convertNullToBlank(l_objResultSet.getString("updatetype_glr"),false).equalsIgnoreCase("ETA")){
////                                            l_dblDist = 0.00;                                                    
////                                        }
//
//                                    }
//
//                                    if (l_intRowCountBefore <= l_intTotalCount) {
//                                        l_objResultSet.absolute(l_intRowCountBefore);
//                                        //before pos 
//                                        Curr = l_objResultSet.getRow();
//                                    }
//
//                                    if (!l_strDates.equals(SDCommonUtil.convertNullToBlank(l_objResultSet.getString("lastupdated"), false))) {
//                                        l_dblTotalDist = l_dblTotalDist + l_dblDist;
////                                    l_dblTotalDist = Math.round(l_dblTotalDist);
//                                        l_dblTotalDist = Double.parseDouble(DF.format(l_dblTotalDist));
//                                        if (l_dblTotalDist == 0.00) {
//                                            l_strTotalDistance = "0 Km";
//                                            l_dblTotalDist = 0.00;
//                                        } else {
//                                            l_strTotalDistance = l_dblTotalDist + " Km";
//                                            l_dblTotalDist = 0.00;
//                                        }
////                                            ++l_intCount;
////                                    l_dblTotalDist = 0.0;
//                                    }
//
//                                    l_objResultSet.absolute(l_intRowCountCurr);
//                                    //curr pos 
//                                    Curr = l_objResultSet.getRow();
//
//                                    if (Curr == l_intTotalCount) {
//                                        l_dblTotalDist = l_dblTotalDist + l_dblDist;
////                                    l_dblTotalDist = Math.round(l_dblTotalDist);
//                                        l_dblTotalDist = Double.parseDouble(DF.format(l_dblTotalDist));
////                                            if(l_intCount == 0){
//                                        if (l_dblTotalDist == 0.00) {
//                                            l_strTotalDistance = "0 Km";
//                                            l_dblTotalDist = 0.00;
//                                        } else {
////                                            if(l_dblTotalDist < 1.0){
////                                                l_strTotalDistance = l_dblTotalDist + " m";
////                                                l_dblTotalDist = 0.0;
////                                            } else{
//                                            l_strTotalDistance = l_dblTotalDist + " Km";
//                                            l_dblTotalDist = 0.00;
////                                            }
//                                        }
////                                            }
//                                    }
//
//                                    if (l_dblDist == 0.0) {
//                                        l_strDistance = "0 Km";
//                                    } else {
////                                    if(l_dblDist < 1.0){
////                                        l_strDistance = l_dblDist + " m";
////                                    } else{
//                                        l_strDistance = l_dblDist + " Km";
////                                    }
//                                    }
//                                    
////                                    if(SDCommonUtil.convertNullToBlank(l_objResultSet.getString("updatetype_glr"),false).equalsIgnoreCase("ETA")){
////                                        l_strDistance = "0 Km";
////                                    }
//
//                                    cellF1.setCellValue(SDCommonUtil.convertNullToBlank(l_strDistance, !l_blnQuotes));
//                                    cellG1.setCellValue(SDCommonUtil.convertNullToBlank(l_strTotalDistance, !l_blnQuotes));
//                                    cellH1.setCellValue(SDCommonUtil.convertNullToBlank(l_objResultSet.getString("accuracy_glr"), !l_blnQuotes));
//                                    cellI1.setCellValue(SDCommonUtil.convertNullToBlank(l_objResultSet.getString("altitude_glr"), !l_blnQuotes));
//                                    cellJ1.setCellValue(SDCommonUtil.convertNullToBlank(l_objResultSet.getString("elapsedtime_glr"), !l_blnQuotes));
//                                    cellK1.setCellValue(SDCommonUtil.convertNullToBlank(l_objResultSet.getString("provider_glr"), !l_blnQuotes));
//
//                                    rowCount--;
//                                }
//                            }
//                        }
//                    } catch (MySQLSyntaxErrorException ex) {
//                        if (ex.getErrorCode() == 1146) {
//                            //Table doesn't exist. 
//                            //We checking if table exists and if not we are continuing with the loop for other tables.
//                        }
//                    }
//                }
//                workbook.write(out);
//                out.close();
//                String path = "_" + "EngineerLocationReport" + ".xlsx";
//
//                ServletOutputStream Servletout = null;
//
//                try {
//                    Servletout = response.getOutputStream();
//
//                } catch (IOException ex) {
//                    Logger.getLogger(FileDownLoadAction.class.getName()).log(Level.SEVERE, null, ex);
//                } catch (Exception ex) {
//                    ex.printStackTrace();
//                }
//
//                response.setContentType("APPLICATION/OCTET-STREAM");
//                response.setHeader("Content-Disposition", "attachment; filename=\"" + path);
//
//                FileInputStream in2 = new FileInputStream(filedirectory + path);
//                if (in2 != null) {
//                    int length;
//                    int bufferSize = 1024;
//                    byte[] buffer = new byte[bufferSize];
//                    while ((length = in2.read(buffer)) != -1) {
//                        Servletout.write(buffer, 0, length);
//                    }
//                    in2.close();
////                Servletout.flush();
//                    Servletout.close();
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//    
    
    //modify by jivaram INF ID:-3982
    //date:-04-03-2023
    private void downloadEngineerGeoLocation(GeoLocationActionForm l_objForm, HttpServletResponse response) {
        try{

            boolean l_blnQuotes = true;
            ArrayList l_lstColumnHeader = new ArrayList();

            l_lstColumnHeader.add(0, "Last Updated Date");
            l_lstColumnHeader.add(1, "Devicets");
            l_lstColumnHeader.add(2, "Lat");
            l_lstColumnHeader.add(3, "Lon");
            l_lstColumnHeader.add(4, "Address");
            l_lstColumnHeader.add(5, "Pointers");
            l_lstColumnHeader.add(6, "Concern Ticket ID");
            l_lstColumnHeader.add(7, "Distance Travelled Poll to Poll");
            l_lstColumnHeader.add(8, "Total Distance Travelled (PD)");
            l_lstColumnHeader.add(9, "Accuracy");
            l_lstColumnHeader.add(10, "Altitude");
            l_lstColumnHeader.add(11, "Elapsed Time");
            l_lstColumnHeader.add(12, "Provider");

                // Create workbook
                String filedirectory = System.getProperty("catalina.base");
                //commented by jivaram INF ID:- 3872
                //date:-30-01-2023
//                filedirectory = filedirectory + "/webapps/EngineerLocationReport/";
                File dir = new File(filedirectory + "/webapps/EngineerLocationReport/");
                if (dir.isDirectory() == false) {
                    dir.mkdir();
                }
                // Work book create
                Workbook workbook = new SXSSFWorkbook();

                FileOutputStream out = new FileOutputStream(new File(filedirectory + "_" + "EngineerLocationReport" + ".xlsx"));

//            FileOutputStream out = new FileOutputStream(new File(filedirectory + "CustomerActivity.xlsx"));
                SXSSFSheet sheet = (SXSSFSheet) workbook.createSheet("EngineerLocationReport_Reports");

                // set Font for column for 1st sheet
                Font font = workbook.createFont();
                font.setColor(IndexedColors.WHITE.getIndex());
                font.setFontHeightInPoints((short) (10.5));
                font.setFontName("Zurich BT");
                font.setBoldweight((short) 25);

                CellStyle cellStyle1 = null;
                cellStyle1 = workbook.createCellStyle();
                cellStyle1.setBorderBottom(CellStyle.BORDER_THIN);
                cellStyle1.setBorderLeft(CellStyle.BORDER_THIN);
                cellStyle1.setBorderRight(CellStyle.BORDER_THIN);
                cellStyle1.setBorderTop(CellStyle.BORDER_THIN);
                cellStyle1.setBottomBorderColor(IndexedColors.BLACK.getIndex());
                cellStyle1.setLeftBorderColor(IndexedColors.BLACK.getIndex());
                cellStyle1.setRightBorderColor(IndexedColors.BLACK.getIndex());
                cellStyle1.setTopBorderColor(IndexedColors.BLACK.getIndex());
                cellStyle1.setAlignment(cellStyle1.ALIGN_CENTER);
                cellStyle1.setFillForegroundColor(IndexedColors.BLACK.getIndex());
                cellStyle1.setFillPattern(CellStyle.SOLID_FOREGROUND);
                cellStyle1.setFillPattern(CellStyle.SOLID_FOREGROUND);
                cellStyle1.setFont(font);

                long rowCount = 1;
                int SheetCount = 0;
                Row row = sheet.createRow((short) 0);
                Cell cell = row.createCell((short) 0);
                Row rowHeader = sheet.createRow((short) 0);
                for (int K = 0; K < l_lstColumnHeader.size(); K++) {
                    cell = rowHeader.createCell((short) K);
                    cell.setCellValue(l_lstColumnHeader.get(K).toString());
                    cell.setCellStyle(cellStyle1);
                    sheet.autoSizeColumn(K);

                }
                Collections.reverse(l_objForm.getM_lstGeoLocation());
                Iterator it = l_objForm.getM_lstGeoLocation().iterator();
                while (it.hasNext()) {
                    EngineerGeoLocationBean l_objEngineerGeoLocationBean = (EngineerGeoLocationBean) it.next();
                    
                    Row RowValue = sheet.createRow((int) (long) rowCount);
                    Cell cellA1 = RowValue.createCell((short) 0);
                    Cell cellB1 = RowValue.createCell((short) 1);
                    Cell cellC1 = RowValue.createCell((short) 2);
                    Cell cellD1 = RowValue.createCell((short) 3);
                    Cell cellE1 = RowValue.createCell((short) 4);
                    Cell cellR1 = RowValue.createCell((short) 5);
                    Cell cellP1 = RowValue.createCell((short) 6);
                    Cell cellF1 = RowValue.createCell((short) 7);
                    Cell cellG1 = RowValue.createCell((short) 8);
                    Cell cellH1 = RowValue.createCell((short) 9);
                    Cell cellI1 = RowValue.createCell((short) 10);
                    Cell cellJ1 = RowValue.createCell((short) 11);
                    Cell cellK1 = RowValue.createCell((short) 12);
                    
                    cellA1.setCellValue(SDCommonUtil.convertNullToBlank(l_objEngineerGeoLocationBean.getM_strLastUpdate(), !l_blnQuotes));
                    cellB1.setCellValue(SDCommonUtil.convertNullToBlank(l_objEngineerGeoLocationBean.getM_strDeviceDateTime(), !l_blnQuotes));
                    cellC1.setCellValue(SDCommonUtil.convertNullToBlank(l_objEngineerGeoLocationBean.getM_strLatitude(), !l_blnQuotes));
                    cellD1.setCellValue(SDCommonUtil.convertNullToBlank(l_objEngineerGeoLocationBean.getM_strLongitude(), !l_blnQuotes));
                    cellE1.setCellValue(SDCommonUtil.convertNullToBlank(l_objEngineerGeoLocationBean.getM_strAddress(), !l_blnQuotes));
                    cellR1.setCellValue(SDCommonUtil.convertNullToBlank(l_objEngineerGeoLocationBean.getM_strUpdateType(), !l_blnQuotes));
                    cellP1.setCellValue(SDCommonUtil.convertNullToBlank(l_objEngineerGeoLocationBean.getM_strTicketID(), !l_blnQuotes));
                    cellF1.setCellValue(SDCommonUtil.convertNullToBlank(l_objEngineerGeoLocationBean.getM_strPollToPollDistance(), !l_blnQuotes));
                    cellG1.setCellValue(SDCommonUtil.convertNullToBlank(l_objEngineerGeoLocationBean.getM_strTotalPollToPollDistance(), !l_blnQuotes));
                    cellH1.setCellValue(SDCommonUtil.convertNullToBlank(l_objEngineerGeoLocationBean.getM_strAccuracy(), !l_blnQuotes));
                    cellI1.setCellValue(SDCommonUtil.convertNullToBlank(l_objEngineerGeoLocationBean.getM_strAltitude(), !l_blnQuotes));
                    cellJ1.setCellValue(SDCommonUtil.convertNullToBlank(l_objEngineerGeoLocationBean.getM_strElapsedTime(), !l_blnQuotes));
                    cellK1.setCellValue(SDCommonUtil.convertNullToBlank(l_objEngineerGeoLocationBean.getM_strProvider(), !l_blnQuotes));
                    l_objEngineerGeoLocationBean = null;
                    rowCount++;
                    
                }
                workbook.write(out);
                out.close();
                String path = "_" + "EngineerLocationReport" + ".xlsx";

                ServletOutputStream Servletout = null;

                try {
                    Servletout = response.getOutputStream();
                } catch (IOException ex) {
                    Logger.getLogger(FileDownLoadAction.class.getName()).log(Level.SEVERE, null, ex);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                response.setContentType("APPLICATION/OCTET-STREAM");
                response.setHeader("Content-Disposition", "attachment; filename=\"" + path);

                FileInputStream in2 = new FileInputStream(filedirectory + path);
                if (in2 != null) {
                    int length;
                    int bufferSize = 1024;
                    byte[] buffer = new byte[bufferSize];
                    while ((length = in2.read(buffer)) != -1) {
                        Servletout.write(buffer, 0, length);
                    }
                    in2.close();
//                Servletout.flush();
                    Servletout.close();
                }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void l_strMsg(String date_should_be__equal_to_2_) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Date: 04/01/2021 Author: Pratig Sonar Desc: function to get list of
     * months and year specified from date range to current date. geolocreg_glr
     * contains data of current month. Input: Start Date Output: List of months
     * and year in MMM_yyyy format and geolocreg_glr table at the end of the
     * list.
     */
    private List<String> getListMonths(String l_strStartDate, String l_strEndDate) {
        List<String> l_lstDates = new ArrayList<>();
        l_lstDates.add("");
        if (!l_strStartDate.isEmpty() && !l_strStartDate.equals("")
                && !l_strEndDate.isEmpty() && !l_strEndDate.equals("")) {
            String l_strFromDate = l_strStartDate;
            String l_strToDate = l_strEndDate;
            if (!"".equals(l_strFromDate.trim()) && !"".equals(l_strToDate.trim())) {
                DateTimeFormatter l_strDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);
                DateTimeFormatter l_strDBTableNameFormatter = DateTimeFormatter.ofPattern("MMMM_yyyy", Locale.ENGLISH);
                LocalDate l_ldStartDate = LocalDate.parse(l_strFromDate, l_strDateFormatter);
                LocalDate l_ldEndDate = LocalDate.parse(l_strToDate, l_strDateFormatter);
                //by jivaram INF ID:-3982 date:-04-03-2023
                //increase the value from 1 to 12 months some monthwise tables are not created
                while (l_ldStartDate.isBefore(l_ldEndDate.plusMonths(12))) {
                    l_lstDates.add("_" + l_ldStartDate.format(l_strDBTableNameFormatter));
                    l_ldStartDate = l_ldStartDate.plusMonths(1);
                }
            }
        }
//        Collections.reverse(l_lstDates);
        return l_lstDates;
    }

    private float getDiffOfLatLon(String l_strFromLatLon, String l_strToLatLon) {

        try {
            int meterConversion = 1609;
            double dist = 0;
            String[] l_arrEnggLatLon = l_strFromLatLon.split(",");
            String[] l_arrCustLatLon = l_strToLatLon.split(",");
            float lat1 = Float.parseFloat(l_arrEnggLatLon[0].trim());
            float lng1 = Float.parseFloat(l_arrEnggLatLon[1].trim());
            float lat2 = Float.parseFloat(l_arrCustLatLon[0].trim());
            float lng2 = Float.parseFloat(l_arrCustLatLon[1].trim());

            if (lat1 == 0 && lng1 == 0) {
                return 0.0f;
            }
            if (lat2 == 0 && lng2 == 0) {
                return 0.0f;
            }


//            System.out.println(lat1+"  "+lng1+"  "+lat2+"  "+lng2);
            try {
                double earthRadius = 3958.75;
                double dLat = Math.toRadians(lat2 - lat1);
                double dLng = Math.toRadians(lng2 - lng1);
                double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                        + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLng / 2) * Math.sin(dLng / 2);
                double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
                dist = earthRadius * c;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new Float(dist * meterConversion).floatValue();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0.0f;
    }
    private static final DecimalFormat DF = new DecimalFormat("0.00");
}
