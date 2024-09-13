package br.com.sankhya.telecontrol;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class FaturaNotaTelecontrol implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao ctx) throws Exception {
		
		try {
			Integer numeroNota = (Integer) ctx.getParam("NUNOTA");
			BigDecimal nota = new BigDecimal(numeroNota);
			putFaturamento(ctx, nota);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
public void putFaturamento(ContextoAcao ctx, BigDecimal nota) throws Exception{
		

		EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = entityFacade.getJdbcWrapper();
	    PreparedStatement pstmt = null;
	    ResultSet rs = null; 
	    
	    BigDecimal id = BigDecimal.ZERO;
	    String nunota = null;
	    String faturado = null;
	    
	    try {
	        jdbc.openSession();
	        
	        String query = "SELECT ID, NUNOTA, FATURADO FROM AD_INFOCABTELECONTROL WHERE NUNOTA =" + nota;

	        //String query = "SELECT ID, NUNOTA, FATURADO FROM AD_INFOCABTELECONTROL WHERE FATURADO = 'N'";
	        //String query = "SELECT ID, NUNOTA, FATURADO FROM AD_INFOCABTELECONTROL WHERE NUNOTA = 2398523";
	        
	        pstmt = jdbc.getPreparedStatement(query);
	        
	        rs = pstmt.executeQuery();
	        
	        while(rs.next()){
	        	
	        	id = rs.getBigDecimal("ID");
	        	nunota = rs.getString("NUNOTA");
	        	faturado = rs.getString("FATURADO");
	        	
	        	if(validarFaturamento(new BigDecimal(nunota))){
	        	
	        		faturarPedido(ctx, "http://api2.telecontrol.com.br/posvenda-faturamento/faturamentos", new BigDecimal(nunota));
	        		
	        	}
	        	
	        }
	        
	    } catch (SQLException e) {
	        e.printStackTrace();
	        throw e;
	    } finally {
	        if (rs != null) {
	            rs.close();
	        }
	        if (pstmt != null) {
	            pstmt.close();
	        }
	        jdbc.closeSession();
	    }
		
		
	}
	
public boolean validarFaturamento(BigDecimal nunota) throws Exception{
	
	
	EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();
	JdbcWrapper jdbc = entityFacade.getJdbcWrapper();
	PreparedStatement pstmt = null;
	ResultSet rs = null; 
	
	BigDecimal nunotaOrig = BigDecimal.ZERO;
	
	try {
		jdbc.openSession();
		
		String query = "SELECT TGFVAR.NUNOTA "
				+"		FROM SANKHYA.TGFVAR "
				+"		WHERE TGFVAR.NUNOTAORIG = ? "
				+"		  AND TGFVAR.STATUSNOTA = 'L' "
				+"		  AND (SELECT TGFCAB.STATUSNFE FROM SANKHYA.TGFCAB "
				+"       WHERE TGFCAB.NUNOTA = TGFVAR.NUNOTA) = 'A' "
				+"		UNION "
				+"		  (SELECT 0 "
				+"		   FROM sankhya.DUAL "
				+"		   WHERE NOT EXISTS "
				+"		       (SELECT TGFVAR.NUNOTA "
				+"		FROM SANKHYA.TGFVAR "
				+"		WHERE TGFVAR.NUNOTAORIG = ? "
				+"		  AND TGFVAR.STATUSNOTA = 'L' "
				+"		  AND (SELECT TGFCAB.STATUSNFE FROM SANKHYA.TGFCAB "
				+"      WHERE TGFCAB.NUNOTA = TGFVAR.NUNOTA) = 'A'))";
		
		pstmt = jdbc.getPreparedStatement(query);
		pstmt.setBigDecimal(1, nunota);
		pstmt.setBigDecimal(2, nunota);
		
		rs = pstmt.executeQuery();
		
		while(rs.next()){
			
			nunotaOrig = rs.getBigDecimal("NUNOTA");
		}
		
	} catch (SQLException e) {
		e.printStackTrace();
		throw e;
	} finally {
		if (rs != null) {
			rs.close();
		}
		if (pstmt != null) {
			pstmt.close();
		}
		jdbc.closeSession();
	}
	
	if(nunotaOrig.compareTo(BigDecimal.ZERO) != 0){
		return true;
	}else{
		return false;
	}
}

public void faturarPedido(ContextoAcao ctx, String ur, BigDecimal nunota) throws Exception {
	
    EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();
	JdbcWrapper jdbc = entityFacade.getJdbcWrapper();
	PreparedStatement pstmt = null;
	ResultSet rs = null;
	String key = preferenciaSenha();

    JsonArray itensArray = new JsonArray();
    JsonObject requestBody = new JsonObject();
  
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S");
    LocalDateTime dateTime = null;
   
	try{
		jdbc.openSession();
		
		 // Fazendo o select na tabela TGFITE
		    String sql = "SELECT \r\n"
		    		+ "(SELECT ISNULL(AD_PEDIDO_TELECONTROL, 0) FROM SANKHYA.TGFCAB WHERE NUNOTA = ?) AS PEDIDO,  \r\n"
		    		+ "ISNULL(AD_PEDIDO_ITEM_TELECONTROL, 0) AS PEDIDOITEM,    \r\n"
		    		+ "(SELECT REFERENCIA FROM SANKHYA.TGFPRO WHERE TGFPRO.CODPROD = TGFITE.CODPROD) AS CODPROD, \r\n"
		    		+ "QTDNEG,  \r\n"
		    		+ "VLRUNIT,    \r\n"
		    		+ "BASEICMS,  \r\n"
		    		+ "VLRICMS,   \r\n"
		    		+ "ALIQICMS,   \r\n"
		    		+ "BASEIPI,  \r\n"
		    		+ "VLRIPI,   \r\n"
		    		+ "ALIQIPI,  \r\n"
		    		+ "BASESUBSTIT,  \r\n"
		    		+ "VLRSUBST   \r\n"
		    		+ "FROM    \r\n"
		    		+ "SANKHYA.TGFITE   \r\n"
		    		+ "WHERE NUNOTA IN   \r\n"
		    		+ "(SELECT TOP 1 TGFVAR.NUNOTA FROM SANKHYA.TGFVAR WHERE TGFVAR.NUNOTAORIG = ?)";
		    
		    pstmt = jdbc.getPreparedStatement(sql);
		    pstmt.setBigDecimal(1, nunota);
		    pstmt.setBigDecimal(2, nunota);
		    rs = pstmt.executeQuery();
		    
		
		    // Percorrendo o ResultSet e fazendo o POST
		    while (rs.next()) {
		    	JsonObject item = new JsonObject();
		    	
		    	
		    	item.addProperty("pedido", rs.getString("PEDIDO").trim());
		    	item.addProperty("pedidoItem", rs.getString("PEDIDOITEM").trim());
		    	item.addProperty("peca", rs.getString("CODPROD").trim());
		    	item.addProperty("quantidade", rs.getString("QTDNEG").trim());
		    	item.addProperty("preco", rs.getString("VLRUNIT").trim());
		    	item.addProperty("baseIcms", rs.getString("BASEICMS").trim());
		    	item.addProperty("valorIcms", rs.getString("VLRICMS").trim());
		    	item.addProperty("aliquotaIcms", rs.getString("ALIQICMS").trim());
		    	item.addProperty("baseIpi", rs.getString("BASEIPI").trim());
		    	item.addProperty("valorIpi", rs.getString("VLRIPI").trim());
		    	item.addProperty("aliquotaIpi", rs.getString("ALIQIPI").trim());
		    	item.addProperty("baseSubsTributaria", rs.getString("BASESUBSTIT").trim());
		    	item.addProperty("valorSubsTributaria", rs.getString("VLRSUBST").trim());
		    	item.addProperty("pecaPedida", rs.getString("CODPROD").trim());
		    	
		    	itensArray.add(item);
		    }
	}catch (Exception e) {
        e.printStackTrace();
    } finally {
        if (rs != null) {
            rs.close();
        }
        if (pstmt != null) {
            pstmt.close();
        }
        jdbc.closeSession();
    }
	
	
	System.out.println("ARRAY ITENS: " + itensArray);

	try{
		jdbc.openSession();
		
		 // Fazendo o select na tabela TGFCAB
		    String sql = "SELECT\r\n"
		    		+ "(SELECT CGC_CPF FROM SANKHYA.TGFPAR WHERE CODPARC = TGFCAB.CODPARC) AS CNPJ, \r\n"
		    		+ "NUMNOTA, \r\n"
		    		+ "SERIENOTA, \r\n"
		    		+ "VLRNOTA, \r\n"
		    		+ "VLRIPI, \r\n"
		    		+ "VLRICMS, \r\n"
		    		+ "DTFATUR, \r\n"
		    		+ "'' AS CFOP, \r\n"
		    		+ "'' AS TRANSPORTADORA, \r\n"
		    		+ "'' AS VALORFRETE, \r\n"
		    		+ "'' AS ENVIO, \r\n"
		    		+ "'' AS CODIGORASTREEIO \r\n"
		    		+ "FROM SANKHYA.TGFCAB \r\n"
		    		+ "WHERE NUNOTA IN  \r\n"
		    		+ "(SELECT TGFVAR.NUNOTA FROM SANKHYA.TGFVAR WHERE TGFVAR.NUNOTAORIG = ?)";
		    
		    pstmt = jdbc.getPreparedStatement(sql);
		    pstmt.setBigDecimal(1, nunota);
		    rs = pstmt.executeQuery();
		    
		
		    // Percorrendo o ResultSet e fazendo o POST
		    while (rs.next()) {
		    	
		    	dateTime = LocalDateTime.parse(rs.getString("DTFATUR"), formatter);
		    	
		    	LocalDate date = dateTime.toLocalDate();
		    	String dataFormatada = date.toString();
		    	requestBody.addProperty("posto", rs.getString("CNPJ").trim());
		    	requestBody.addProperty("notaFiscal", rs.getString("NUMNOTA").trim());
		    	requestBody.addProperty("serie", rs.getString("SERIENOTA").trim());
		    	requestBody.addProperty("emissao", dataFormatada);
		    	requestBody.addProperty("totalNota", rs.getString("VLRNOTA").trim());
		    	requestBody.addProperty("totalIpi", rs.getString("VLRIPI").trim());
		    	requestBody.addProperty("totalIcms", rs.getString("VLRICMS").trim());
		    	requestBody.addProperty("cfop", getCfop(nunota));
		    	/*requestBody.addProperty("transportadora", "<valor-transportadora>");
		    	requestBody.addProperty("valorFrete", "<valor-valorFrete>");
		    	requestBody.addProperty("envio", "<valor-envio>");
		    	requestBody.addProperty("codigoRastreio", "<valor-codigoRastreio>");*/

		    	
		    }
	}catch (Exception e) {
        e.printStackTrace();
    } finally {
        if (rs != null) {
            rs.close();
        }
        if (pstmt != null) {
            pstmt.close();
        }
        jdbc.closeSession();
    }
    
	requestBody.add("itens", itensArray);

    // Imprimindo o JSON completo que será enviado na requisição
	

	
 // Preparando a requisi��o
    URL obj = new URL(ur);
    HttpURLConnection http = (HttpURLConnection) obj.openConnection();
    
    http.setRequestMethod("POST");
    http.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
    http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
    http.setRequestProperty("Access-Application-Key", key);
	http.setRequestProperty("Access-Env", "PRODUCTION");
    http.setDoOutput(true);
    
    System.out.println("Teste null");

    System.out.println(requestBody);
    System.out.println(http);
    
    // Enviando a requisi��o
    OutputStream os = http.getOutputStream();
    os.write(requestBody.toString().getBytes("UTF-8"));
    os.flush();
    os.close();

    // Recebendo a resposta
    int responseCode = http.getResponseCode();
    System.out.println("Response Code : " + responseCode);
    System.out.println(requestBody);
    ctx.setMensagemRetorno(" " + responseCode + " \n JSON: \n" +  requestBody);

    if(responseCode == 400){
    	updateTableFatur(ctx, nunota);
    	ctx.setMensagemRetorno("Nunota " + nunota + " foi faturada no Telecontrol \n" + " JSON COMPLETO: \n" + requestBody);
    }else{
    	 BufferedReader in = new BufferedReader(new InputStreamReader(http.getInputStream()));
         String inputLine;
         StringBuffer response = new StringBuffer();

         while ((inputLine = in.readLine()) != null) {
             response.append(inputLine);
         }
         
         in.close();

         // Exibindo a resposta
         System.out.println(response.toString());
         
         if(responseCode == 201){
         	updateTableFatur(ctx, nunota);
        	ctx.setMensagemRetorno("Nunota " + nunota + " foi faturada no Telecontrol \n" + " JSON COMPLETO: \n" + requestBody);
         }
    }

    
}

public static String preferenciaSenha() throws Exception {
	
	EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();
	JdbcWrapper jdbc = entityFacade.getJdbcWrapper();
	PreparedStatement pstmt = null;
	String senha = null;
	ResultSet rs = null;

	jdbc.openSession();

	String sqlSenha = "SELECT VALOR FROM SANKHYA.AD_PARAMETROS WHERE "
			+ "PARAMETRO = 'chave api telecontrol'";
	pstmt = jdbc.getPreparedStatement(sqlSenha);

	rs = pstmt.executeQuery();
	while (rs.next()) {

		senha = rs.getString("VALOR");

	}

	System.out.println(senha);

	jdbc.closeSession();

	return senha;

}

public String getCfop(BigDecimal nunota) throws Exception {
	
	EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();
	JdbcWrapper jdbc = entityFacade.getJdbcWrapper();
	PreparedStatement pstmt = null;
	String cfop = null;
	ResultSet rs = null;
	
	jdbc.openSession();
	
	String sqlSenha = "SELECT TOP 1 CODCFO FROM TGFITE WHERE  "
		+"	NUNOTA IN (SELECT TOP 1 NUNOTA FROM TGFVAR WHERE NUNOTAORIG = ?)  "
		+"	UNION (SELECT 0 FROM DUAL WHERE NOT EXISTS (SELECT TOP 1 CODCFO FROM TGFITE WHERE   "
		+"			NUNOTA IN (SELECT TOP 1 NUNOTA FROM TGFVAR WHERE NUNOTAORIG = ?)))";
	pstmt = jdbc.getPreparedStatement(sqlSenha);
	pstmt.setBigDecimal(1, nunota);
	pstmt.setBigDecimal(2, nunota);
	
	rs = pstmt.executeQuery();
	while (rs.next()) {
		
		cfop = rs.getString("CODCFO");
		
	}
	
	System.out.println(cfop);
	
	jdbc.closeSession();
	
	return cfop;
	
}

public void updateTableFatur(ContextoAcao ctx, BigDecimal nunota) throws Exception{
	
	EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();
	JdbcWrapper jdbc = entityFacade.getJdbcWrapper();
	PreparedStatement pstmt = null;
	try {
	    jdbc.openSession();

	    String sqlUpd = "UPDATE AD_INFOCABTELECONTROL SET FATURADO = 'S' WHERE NUNOTA = ?";

	    pstmt = jdbc.getPreparedStatement(sqlUpd);
	    pstmt.setBigDecimal(1, nunota);
	    
	    pstmt.executeUpdate();

	} catch (SQLException e) {
	    e.printStackTrace();
	} finally {
	    if (pstmt != null) {
	        pstmt.close();
	    }
	    jdbc.closeSession();
	}
	
}



}
