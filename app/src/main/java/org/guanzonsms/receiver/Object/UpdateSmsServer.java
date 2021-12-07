package org.guanzonsms.receiver.Object;

import android.app.Application;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import org.guanzonsms.receiver.Callback.UpdateInstance;
import org.guanzonsms.receiver.Callback.UpdateSmsServerCallback;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

public class UpdateSmsServer implements UpdateInstance {
    private static final String TAG = UpdateSmsServer.class.getSimpleName();
    private final Application poApp;
    private final RAreaPerformance poDataBse;
    private final ConnectionUtil poConn;
    private final HttpHeaders poHeaders;

    public Import_AreaPerformance(Application application) {
        this.poApp = application;
        this.poDataBse = new RAreaPerformance(application);
        this.poConn = new ConnectionUtil(application);
        this.poHeaders = HttpHeaders.getInstance(application);
    }

    @Override
    public void onUpdateServer(UpdateSmsServerCallback callback) {
        try{
            new ImportAreaTask(poApp, callback, poHeaders, poDataBse, poConn).execute(BranchPerformancePeriod.getList());
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private static class ImportAreaTask extends AsyncTask<ArrayList<String>, Void, String> {
        private final ImportDataCallback callback;
        private final HttpHeaders loHeaders;
        private final RAreaPerformance loDatabse;
        private final ConnectionUtil loConn;
        private final REmployee poUser;

        public ImportAreaTask(Application foApp, ImportDataCallback callback, HttpHeaders loHeaders, RAreaPerformance loDatabse, ConnectionUtil loConn) {
            this.callback = callback;
            this.loHeaders = loHeaders;
            this.loDatabse = loDatabse;
            this.loConn = loConn;
            this.poUser = new REmployee(foApp);
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        protected String doInBackground(ArrayList<String>... arrayLists) {
            String response = "";
            try {
                if(loConn.isDeviceConnected()) {
                    if(arrayLists[0].size() > 0) {
                        for(int x = 0 ; x < arrayLists[0].size(); x++) {
                            JSONObject loJSon = new JSONObject();
                            loJSon.put("period", arrayLists[0].get(x));
                            loJSon.put("areacd", poUser.getUserAreaCode());
                            response = WebClient.httpsPostJSon(IMPORT_AREA_PERFORMANCE, loJSon.toString(), loHeaders.getHeaders());
                            JSONObject loJson = new JSONObject(response);
                            Log.e(TAG, loJson.getString("result"));
                            String lsResult = loJson.getString("result");
                            if (lsResult.equalsIgnoreCase("success")) {
                                JSONArray laJson = loJson.getJSONArray("detail");
                                saveDataToLocal(laJson);
                            } else {
                                JSONObject loError = loJson.getJSONObject("error");
                                String message = loError.getString("message");
                                callback.OnFailedImportData(message);
                            }
                            Thread.sleep(1000);
                        }
                    }
                } else {
                    response = AppConstants.NO_INTERNET();
                }
            } catch (Exception e) {
                Log.e(TAG, Arrays.toString(e.getStackTrace()));
                e.printStackTrace();
            }
            return response;
        }

        void saveDataToLocal(JSONArray laJson) throws Exception{
            List<EAreaPerformance> areaInfos = new ArrayList<>();
            for(int x = 0; x < laJson.length(); x++){
                JSONObject loJson = new JSONObject(laJson.getString(x));
                EAreaPerformance info = new EAreaPerformance();
                info.setPeriodxx(loJson.getString("sPeriodxx"));
                info.setAreaCode(loJson.getString("sAreaCode"));
                info.setAreaDesc(loJson.getString("sAreaDesc"));
                info.setMCGoalxx(Integer.parseInt(loJson.getString("nMCGoalxx")));
                info.setSPGoalxx(Float.parseFloat(loJson.getString("nSPGoalxx")));
                info.setJOGoalxx(Integer.parseInt(loJson.getString("nJOGoalxx")));
                info.setLRGoalxx(Float.parseFloat(loJson.getString("nLRGoalxx")));
                info.setMCActual(Integer.parseInt(loJson.getString("nMCActual")));
                info.setSPActual(Float.parseFloat(loJson.getString("nSPActual")));
                info.setLRActual(Float.parseFloat(loJson.getString("nLRActual")));
                areaInfos.add(info);
            }
            loDatabse.insertBulkData(areaInfos);
            Log.e(TAG, "Branch info has been save to local.");
        }
    }
}
