
import com.commons.Constants;
import com.dataaccess.DataAccess;
import com.mysql.jdbc.exceptions.jdbc4.MySQLSyntaxErrorException;
import com.utils.SDCommonUtil;
import java.io.IOException;
import java.io.PrintWriter;
import static java.lang.Math.round;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.ActionForm;
import org.json.JSONObject;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author user
 */
public class EngineerLocation extends HttpServlet {

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            /* TODO output your page here. You may use following sample code. */
            out.print(this.getJSONString(request));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

    private static final DecimalFormat DF = new DecimalFormat("0.00"); 
    
    private JSONObject getJSONString(HttpServletRequest request) {
        JSONObject l_objJSON = new JSONObject();
        try {

            Connection l_objConnection = DataAccess.connectToDatabase();
            Statement l_objStatement = l_objConnection.createStatement();
            Statement l_objStatement1 = l_objConnection.createStatement();
            Statement l_objStatement2 = l_objConnection.createStatement();
            if (request.getParameter("action") != null) {
                Map m1 = new HashMap();
                List l1 = new LinkedList();
                String EndDate = "";
//                String l_strDownCallID = SDCommonUtil.convertValuesForValueAndID(l_objStatement, Constants.DB_NAME + ".categorymst_cm", "typevalue_cm", "typeid_cm", SDCommonUtil.convertBlankToNull("Down", true), false);
                String l_strEngineerName = request.getParameter("EngineerName");
                String l_strStartDate = request.getParameter("StartDate");
                String l_strEndDate = request.getParameter("EndDate");
                String l_strRole = SDCommonUtil.convertNullToBlank(request.getParameter("Role"), true);
                int EnggID = Integer.parseInt(SDCommonUtil.convertNullToBlank(request.getParameter("UserID"), true));
//                String l_strEndDate = request.getParameter("EndDate");
//                String l_strEngineerName = "Alwin Vaz";
//                    String l_strBusinessUnitId = SDCommonUtil.convertBlankToNull(SDCommonUtil.convertValuesForValueAndIDWithDeleteFlag(l_objStatement1, Constants.DB_NAME + ".businessunitmst_bum", "typevalue_bum", "typeid_bum", "'" + l_strBusinessUnit + "'", "deleteflag_bum", false), false);
//                    String l_strPrincipleCustomer = request.getParameter("PrincipleCustomer");
//                    String l_strCustomerName = request.getParameter("CustomerName");

                String Date = "", Time = "";
                String l_strQuery = "select curdate(),curtime()";
                ResultSet l_objResultSet = l_objStatement.executeQuery(l_strQuery);
                if (l_objResultSet != null) {
                    while (l_objResultSet.next()) {
                        Date = l_objResultSet.getString(1);
                        Time = l_objResultSet.getString(2);
                    }
                }
//                if (l_strStartDate != null && l_strStartDate.equals("") == false) {
//                    String l_strQuery1 = "SELECT DATE_ADD('" + l_strStartDate + "', INTERVAL 1 MONTH)";
//                    ResultSet l_objResultSet1 = l_objStatement.executeQuery(l_strQuery1);
//                    if (l_objResultSet1 != null) {
//                        while (l_objResultSet1.next()) {
//                            EndDate = l_objResultSet1.getString(1);
//                        }
//                    }
//                }
               EndDate = l_strEndDate;
                //Geo Location For Engineer
                if (request.getParameter("action").equals("GeoLocation")) {
                    int l_intTypeIDEngineer = 0;
                    if (l_strRole.equals("Technician")) {
                        l_intTypeIDEngineer = EnggID;
                    } else {
                        String l_strQueryGetEID = "select typeid_em from " + Constants.DB_NAME + ".engineermst_em where concat(fname_em,' ',lname_em)='" + l_strEngineerName + "' "
                                + " AND deleteflag_em='N' and resignedflag_em='N' and role_rm_em IN(2,15) and (emprole_erm_em is null or emprole_erm_em='')";
                        l_objResultSet = l_objStatement.executeQuery(l_strQueryGetEID);
                        if (l_objResultSet != null) {
                            while (l_objResultSet.next()) {
                                l_intTypeIDEngineer = l_objResultSet.getInt("typeid_em");
                            }
                        }
                    }
                    if (l_intTypeIDEngineer != 0) {
                        // Iterating geo loc months table according to start date to current date. - By Pratig Sonar - 04/01/2021
                        List<String> l_lstDates = new ArrayList<>();
                        l_lstDates = getListMonths(l_strStartDate, EndDate); //get list of months and year between date range.
                        //Iteration and execution according to date is done after before query execution.
                        // Ends here by Pratig Sonar
                        for (String l_strDate : l_lstDates) {
                            try {
                                //by jivaram discuss with SP sir
                                //leave pointer should not display
                                //date:-02-02-2023
                                int l_intTotalCount = 0;
//                                l_strQuery = "SELECT * FROM " + Constants.DB_NAME + ".geolocreg_glr" + l_strDate + " where "
                                l_strQuery = "SELECT *,substring(lastupdated_glr,1,10) as lastupdated FROM " + Constants.DB_NAME + ".geolocreg_glr" + l_strDate + " where "
                                        + "id_em_glr='" + l_intTypeIDEngineer + "' and DATE(lastupdated_glr) >= DATE('" + l_strStartDate + "') and DATE(lastupdated_glr) <= DATE('" + EndDate + "') and updatetype_glr != 'Leave' order by  devicets_glr asc";

                                String l_strCountQuery = "SELECT count(*) FROM " + Constants.DB_NAME + ".geolocreg_glr" + l_strDate + " where "
                                        + "id_em_glr='" + l_intTypeIDEngineer + "' and DATE(lastupdated_glr) >= DATE('" + l_strStartDate + "') and DATE(lastupdated_glr) <= DATE('" + EndDate + "') and updatetype_glr != 'Leave' order by  devicets_glr asc";
                                l_objResultSet = l_objStatement2.executeQuery(l_strCountQuery);
                                if(l_objResultSet != null){
                                    while(l_objResultSet.next()){
                                        l_intTotalCount = l_objResultSet.getInt(1);
                                    }
                                }
//                        l_strQuery = "SELECT * FROM " + Constants.DB_NAME + ".geolocreg_glr where "
//                                + "id_em_glr='" + l_intTypeIDEngineer + "' and DATE(lastupdated_glr) >= DATE('" + l_strStartDate + "') and DATE(lastupdated_glr) <= DATE('" + EndDate + "') order by  devicets_glr desc";
                                l_objResultSet = l_objStatement.executeQuery(l_strQuery);
                                String l_strDates = "";
                                List l_lstDate = new ArrayList();
                                List l_lstlatlon = new ArrayList();
                                String l_strFromlatlng  = "", l_strTolatlng = "", l_strDistance = "", l_strTotalDistance = "";
                                double l_dblDist = 0.00;
                                double l_dblTotalDist = 0.00;
                                int l_intRowCountCurr = 0;
                                int l_intRowCountBefore = 1;
                                int l_intCount = 0;
                                
                                if (l_objResultSet != null) {
                                    while (l_objResultSet.next()) {
                                        m1 = new HashMap();
                                        l_strTotalDistance = "";
                                        ++l_intRowCountCurr;
                                        ++l_intRowCountBefore;
                                        //curr pos 
                                        int Curr = l_objResultSet.getRow();
                                        
                                        l_strDates = SDCommonUtil.convertNullToBlank(l_objResultSet.getString("lastupdated"), false);
                                        
                                        if(l_lstDate.contains(l_strDates)){
                                            
                                            l_dblTotalDist = l_dblTotalDist + l_dblDist;
//                                            l_dblTotalDist = Math.round(l_dblTotalDist);
                                            l_dblTotalDist = Double.parseDouble(DF.format(l_dblTotalDist));
                                            
                                            
                                            int lastindex = l_lstlatlon.size() - 1;
                                            l_strFromlatlng = l_lstlatlon.get(lastindex).toString();
                                            
                                            l_strTolatlng = SDCommonUtil.convertNullToBlank(l_objResultSet.getString("lat_glr"), false) + "," + SDCommonUtil.convertNullToBlank(l_objResultSet.getString("lon_glr"), false);
                                            
                                        }                                        
                                        l_lstDate.add(l_strDates);                                      
                                        
                                        
                                        String l_strlatlon = SDCommonUtil.convertNullToBlank(l_objResultSet.getString("lat_glr"), false) + "," + SDCommonUtil.convertNullToBlank(l_objResultSet.getString("lon_glr"), false);
                                        l_lstlatlon.add(l_strlatlon);
                                        
                                        m1.put("Lat", SDCommonUtil.convertNullToBlank(l_objResultSet.getString("lat_glr"), false));
                                        m1.put("Lon", SDCommonUtil.convertNullToBlank(l_objResultSet.getString("lon_glr"), false));
                                        m1.put("LastUpdatedDate", SDCommonUtil.convertNullToBlank(l_objResultSet.getString("lastupdated_glr"), false));
                                        m1.put("Devicets", SDCommonUtil.convertNullToBlank(l_objResultSet.getString("devicets_glr"), false));
                                        m1.put("LogID", SDCommonUtil.convertNullToBlank(l_objResultSet.getString("logid_glr"), false));
                                        m1.put("Address", SDCommonUtil.convertNullToBlank(l_objResultSet.getString("address_glr"), false));
                                        m1.put("Pointers", SDCommonUtil.convertNullToBlank(l_objResultSet.getString("updatetype_glr"), false));
                                        m1.put("Concern Ticket ID", SDCommonUtil.convertNullToBlank(l_objResultSet.getString("ticketid_glr"), false));
                                        
                                        //Distance Calculation From two lat lng
                                        if(!l_strFromlatlng.equals("") && !l_strTolatlng.equals("")){
                                            if(!l_strFromlatlng.equals(",") && !l_strTolatlng.equals(",")){

                                                l_dblDist = getDiffOfLatLon(l_strFromlatlng,l_strTolatlng);
                                                l_dblDist = l_dblDist / 1000;
                                                double l_strDistance2 = (l_dblDist * 25) / 100;
                                                l_dblDist = l_dblDist + l_strDistance2;
    //                                            l_dblDist = Math.round(l_dblDist);

                                                l_dblDist =  Double.parseDouble(DF.format(l_dblDist));

                                                if(SDCommonUtil.convertNullToBlank(l_objResultSet.getString("updatetype_glr"),false).equalsIgnoreCase("login")){
                                                    l_dblDist = 0.00;
                                                    l_dblTotalDist = 0.00;
                                                }
//                                                if(SDCommonUtil.convertNullToBlank(l_objResultSet.getString("updatetype_glr"),false).equalsIgnoreCase("ETA")){
//                                                    l_dblDist = 0.00;                                                    
//                                                }

                                            }
                                        }
                                        
                                        if(l_intRowCountBefore <= l_intTotalCount){                                            
                                            l_objResultSet.absolute(l_intRowCountBefore);
                                            //before pos 
                                            Curr = l_objResultSet.getRow();                                        
                                        }
                                        
                                        if(!l_strDates.equals(SDCommonUtil.convertNullToBlank(l_objResultSet.getString("lastupdated"),false)) ){
                                            l_dblTotalDist = l_dblTotalDist + l_dblDist;
//                                            l_dblTotalDist = Math.round(l_dblTotalDist);
                                            l_dblTotalDist = Double.parseDouble(DF.format(l_dblTotalDist));
                                            if(l_dblTotalDist == 0.00){
                                                l_strTotalDistance = "0 Km";
                                                l_dblTotalDist = 0.00;
                                            } else{
                                                l_strTotalDistance = l_dblTotalDist + " Km";
                                                l_dblTotalDist = 0.00;
                                            }
//                                            ++l_intCount;
//                                            l_dblTotalDist = 0.0;
                                        }
                                        
                                        l_objResultSet.absolute(l_intRowCountCurr);
                                        //curr pos 
                                        Curr = l_objResultSet.getRow();
                                        
                                        if(Curr == l_intTotalCount){
                                            l_dblTotalDist = l_dblTotalDist + l_dblDist;
//                                            l_dblTotalDist = Math.round(l_dblTotalDist);
                                            l_dblTotalDist = Double.parseDouble(DF.format(l_dblTotalDist));
//                                            if(l_intCount == 0){
                                                if(l_dblTotalDist == 0.00){
                                                    l_strTotalDistance = "0 Km";
                                                    l_dblTotalDist = 0.00;
                                                } else{
//                                                    if(l_dblTotalDist < 1.0 ){
//                                                        l_strTotalDistance = l_dblTotalDist + " m";
//                                                        l_dblTotalDist = 0.0;
//                                                    } else{
                                                        l_strTotalDistance = l_dblTotalDist + " Km";
                                                        l_dblTotalDist = 0.00;
//                                                    }
                                                }
//                                            }
                                        }
                                        if(l_dblDist == 0.00){
                                            l_strDistance = "0 Km";
                                        } else{
//                                            if(l_dblDist < 1.0){
//                                                l_strDistance = l_dblDist + " m";
//                                            } else{
                                                l_strDistance = l_dblDist + " Km";
//                                            }
                                        }
//                                        if(SDCommonUtil.convertNullToBlank(l_objResultSet.getString("updatetype_glr"),false).equalsIgnoreCase("ETA")){
//                                            l_strDistance = "0 Km";
//                                        }
                                        
                                        m1.put("Distance Travelled Poll to Poll", SDCommonUtil.convertNullToBlank(l_strDistance, false));
                                        m1.put("Total Distance Travelled (PD)", SDCommonUtil.convertNullToBlank(l_strTotalDistance, false));
                                        
                                        m1.put("Accuracy", SDCommonUtil.convertNullToBlank(l_objResultSet.getString("accuracy_glr"), false));
                                        m1.put("Altitude", SDCommonUtil.convertNullToBlank(l_objResultSet.getString("altitude_glr"), false));
                                        m1.put("ElapsedTime", SDCommonUtil.convertNullToBlank(l_objResultSet.getString("elapsedtime_glr"), false));
                                        m1.put("Provider", SDCommonUtil.convertNullToBlank(l_objResultSet.getString("provider_glr"), false));
                                        l1.add(m1);
                                        m1 = null;
                                    }
                                }
                            } catch (MySQLSyntaxErrorException ex) {
                                if (ex.getErrorCode() == 1146) {
                                    //Table doesn't exist. 
                                    //We checking if table exists and if not we are continuing with the loop for other tables.
                                }
                            }
                        }
                    }
                    try {
                        l_objJSON.put("GeoLocationJson", l1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return l_objJSON;
    }
    
    //Distance Calculation Formula
//    private float getDiffOfLatLon(String l_strEnggLatLon, String l_strCustLatLon) {
//        
//        int meterConversion = 1609;
//        double dist = 0;
//
//        String[] l_arrEnggLatLon = l_strEnggLatLon.split(",");
//        String[] l_arrCustLatLon = l_strCustLatLon.split(",");
//        float lat1 = Float.parseFloat(l_arrEnggLatLon[0]);
//        float lng1 = Float.parseFloat(l_arrEnggLatLon[1]);
//        float lat2 = Float.parseFloat(l_arrCustLatLon[0]);
//        float lng2 = Float.parseFloat(l_arrCustLatLon[1]);
//        
//        if(lat1 == 0 && lng1 == 0 ){return 0.0f; }
//        if(lat2 == 0 && lng2 == 0 ){return 0.0f; }
//            
//        try {
//            double earthRadius = 3958.75;
//            double dLat = Math.toRadians(lat2 - lat1);
//            double dLng = Math.toRadians(lng2 - lng1);
//            double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
//                    + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
//                    * Math.sin(dLng / 2) * Math.sin(dLng / 2);
//            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
//            dist = earthRadius * c;
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return new Float(dist * meterConversion).floatValue();
//
//    }
//    
    private float getDiffOfLatLon(String l_strFromLatLon, String l_strToLatLon) {
        
        try{
            int meterConversion = 1609;
            double dist = 0;
            String[] l_arrEnggLatLon = l_strFromLatLon.split(",");
            String[] l_arrCustLatLon = l_strToLatLon.split(",");
            float lat1 = Float.parseFloat(l_arrEnggLatLon[0].trim());
            float lng1 = Float.parseFloat(l_arrEnggLatLon[1].trim());
            float lat2 = Float.parseFloat(l_arrCustLatLon[0].trim());
            float lng2 = Float.parseFloat(l_arrCustLatLon[1].trim());
            
            if(lat1 == 0 && lng1 == 0 ){return 0.0f; }
            if(lat2 == 0 && lng2 == 0 ){return 0.0f; }
            

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
        }
        
        catch(Exception e){
            e.printStackTrace();
        }
        return 0.0f;
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
}
