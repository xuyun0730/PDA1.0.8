package cn.starhelix.material.service;

import cn.starhelix.material.data.BatchListResponse;
import cn.starhelix.material.data.LoginResponse;
import cn.starhelix.material.data.MapDataResponse;
import cn.starhelix.material.data.PremixSubListResponse;
import io.reactivex.rxjava3.core.Single;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface MaterialService {
    @FormUrlEncoded
    @POST("/echain/public/mslJoint.do?action=generateToken4Msl&tenantName=mslscsyxt")
    Single<LoginResponse> login(
            @Field("username") String username,
            @Field("password") String password
    );

    @FormUrlEncoded
    @POST("/echain/thirdParty/mslJoint.do?action=getBatchList")
    Single<BatchListResponse> getBatchList(
            @Query("ecafeToken") String ecafeToken,
            @Field("id") String batchId,
            @Field("batch") String batchName,
            @Field("rp") int rp,
            @Field("page") int page,
            @Field("operationTypeCode") String operationTypeCode,
            @Field("buildControls") String buildControls,
            @Field("serialNumber") String serialNumber
    );

    @FormUrlEncoded
    @POST("/echain/thirdParty/mslJoint.do?action=getFormulaDetail")
    Single<PremixSubListResponse> getPremixSubList(
            @Query("ecafeToken") String ecafeToken,
            @Field("businessVarietyId") String businessVarietyId,
            @Field("relationInfoId") String relationInfoId,
            @Field("operationTypeCode") String operationTypeCode,
            @Field("buildControls") String buildControls,
            @Field("serialNumber") String serialNumber
    );

    @FormUrlEncoded
    @POST("/echain/thirdParty/mslJoint.do?action=saveMaterialLaunchRecord")
    Single<MapDataResponse> saveLaunchRecord(
            @Query("ecafeToken") String ecafeToken,
            @Field("relationInfoId") String relationInfoId,
            @Field("batch") String batch,
            @Field("outputProductId") String outputProductId,
            @Field("serialNumber") int serialNumber,
            @Field("operationTypeCode") String operationTypeCode,
            @Field("operationDate") String operationDate,
            @Field("launchDetailJson") String launchDetailJson,
            @Field("codeContentsJson") String codeContentsJson
    );

    @FormUrlEncoded
    @POST("/echain/thirdParty/mslJoint.do?action=batchSaveMaterialLaunchRecord")
    Single<MapDataResponse> batchSaveLaunchRecord(
            @Query("ecafeToken") String ecafeToken,
            @Field("relationInfoId") String relationInfoId,
            @Field("batch") String batch,
            @Field("outputProductId") String outputProductId,
            @Field("operationTypeCode") String operationTypeCode,
            @Field("launchDetailJson") String launchDetailJson,
            @Field("codeContentsJson") String codeContentsJson
    );

    @FormUrlEncoded
    @POST("/echain/thirdParty/mslJoint.do?action=confirmReceipt")
    Single<MapDataResponse> confirmReceipt(
            @Query("ecafeToken") String ecafeToken,
            @Field("receiptId") String receiptId,
            @Field("clientStatus") String clientStatus
    );

    @FormUrlEncoded
    @POST("/echain/thirdParty/mslJoint.do?action=checkExistCode")
    Single<MapDataResponse> checkQrcode(
            @Query("ecafeToken") String ecafeToken,
            @Field("traceCode") String traceCode
    );
}
