package tw.com.ispan.eeit.ho_back.util;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Date;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jose.crypto.DirectEncrypter;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import jakarta.annotation.PostConstruct;

/**
 * JWT 工具類（使用 nimbus-jose-jwt 庫）
 * 
 * ⚠️ 注意：此工具類與 JwtUtil 不兼容
 * - JsonWebTokenUtils：使用加密 token (JWE)，使用 nimbus-jose-jwt 庫
 * - JwtUtil：使用簽名 token (JWS)，使用 jjwt 庫
 * 
 * 使用場景：
 * - LoginController：用戶端登入
 * - GoogleAuthController：Google 登入
 * - SupportController, SReplyController：客服系統（通過 SupportTokenHelper）
 * 
 * 建議：未來統一 JWT 工具類時，需要遷移所有使用此工具類的地方
 */
@Component
public class JsonWebTokenUtils {
    private long expireTime = 60;
    private String issuer = "YiChen";
    private byte[] sharedSecret = new byte[64];

    public String createJwt(String data) {
        SecureRandom random = new SecureRandom();
        random.nextBytes(sharedSecret);
        Instant now = Instant.now();
        Instant expire = now.plusSeconds(expireTime * 60);
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(expire))
                .subject(data)
                .build();
        // Create HMAC signer
        try {
            // Prepare JWS object payload
            JWSObject jwsObject = new JWSObject(new JWSHeader(JWSAlgorithm.HS512), claimsSet.toPayload());
            // Apply the HMAC
            JWSSigner signer = new MACSigner(sharedSecret);
            jwsObject.sign(signer);
            return jwsObject.serialize();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String validateJwt(String token) {
        try {
            JWSObject jwsObject = JWSObject.parse(token);
            JWTClaimsSet claimsSet = JWTClaimsSet.parse(jwsObject.getPayload().toString());
            JWSVerifier verifier = new MACVerifier(sharedSecret);
            if (jwsObject.verify(verifier) && new Date().before(claimsSet.getExpirationTime())) {
                String data = claimsSet.getSubject();
                return data;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private SecretKey secretKey; // 用在加密

    @PostConstruct
    public void generateSecretKey() {
        // 取得A256GCM演算法所需要的金鑰長度
        int length = EncryptionMethod.A256GCM.cekBitLength();
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(length);
            secretKey = keyGen.generateKey();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new ExceptionInInitializerError(e);
        }
    }

    public String createEncryptedToken(String data) {
        Instant now = Instant.now();
        Instant expire = now.plusSeconds(expireTime * 60);
        try {
            // 建立JWT主體
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(expire))
                    .subject(data)
                    .build();

            // 建立JWS(使用HS512簽章的JWT)：header+主體
            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.HS512),
                    claimsSet);

            // 使用HMAC signer簽章
            signedJWT.sign(new MACSigner(sharedSecret));

            // 建立JWE Payload
            Payload payload = new Payload(signedJWT);

            // 建立JWE
            JWEObject jweObject = new JWEObject(
                    new JWEHeader(JWEAlgorithm.DIR, EncryptionMethod.A256GCM),
                    payload);

            // 使用Encrypter加密
            jweObject.encrypt(new DirectEncrypter(secretKey));

            // 產生Token
            return jweObject.serialize();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String validateEncryptedToken(String token) {
        try {
            // 解析JWE
            JWEObject jweObject = JWEObject.parse(token);

            // 使用Decrypter解密
            jweObject.decrypt(new DirectDecrypter(secretKey));

            // 取得JWE Payload
            Payload payload = jweObject.getPayload();

            // 取得JWS
            SignedJWT signedJWT = payload.toSignedJWT();

            // 取得JWT主體
            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();

            // 建立HMAC verifier
            JWSVerifier verifier = new MACVerifier(sharedSecret);

            // 驗證簽章 + 驗證過期時間
            if (signedJWT.verify(verifier) && new Date().before(claimsSet.getExpirationTime())) {
                String subject = claimsSet.getSubject();
                return subject;
            } else {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "jwt token失效");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
