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
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class AttPrecoPecas implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao ctx) throws Exception {

		try {
			cadTabPrecoPecas(ctx);
		} catch (Exception e) {
			e.printStackTrace();
		}
		//cadTabPrecoProd();
		
	}
	
	
	
	
	public void cadTabPrecoPecas(ContextoAcao ctx) throws Exception{
		EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = entityFacade.getJdbcWrapper();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		//http://api2.telecontrol.com.br/posvenda-core/tabelaPreco
		
		BigDecimal valor = BigDecimal.ZERO;
		BigDecimal codprod = BigDecimal.ZERO;
		BigDecimal codtab = BigDecimal.ZERO;
		String referencia = null;
		
		Map<String, String> parametro = obterParametro();
		
		System.out.println("Teste tabPre�o: " + parametro.get("tabPreco"));
		
		codtab = new BigDecimal(parametro.get("tabPreco"));
		
		try {
			jdbc.openSession();
			
			String query = "SELECT CODPROD, REFERENCIA FROM SANKHYA.TGFPRO\r\n"
					+ "WHERE AD_ENVIADOTELECONTROL = 1\r\n"
					+ "AND AD_PRODUTOTELECONTROL = 1\r\n"
					+ "AND REFERENCIA = 7898511034668 )";
			
			pstmt = jdbc.getPreparedStatement(query);
			rs = pstmt.executeQuery();
			
			while(rs.next()){
				
				codprod = rs.getBigDecimal("CODPROD");
				
				referencia = rs.getString("REFERENCIA");
				
				valor = getVlrUnit(codprod, codtab);
				
				System.out.println("Valor " + valor + " do produto " + codprod);
				
				if(valor.compareTo(BigDecimal.ZERO) != 0 && codprod.compareTo(BigDecimal.ZERO) != 0){
					cadTabPrecoGar(ctx, referencia, valor);
					cadTabPrecoVen(ctx, referencia, valor);
					System.out.println("Produto cadastrado");
				}else{
					System.out.println("PreÇo � igual a 0");
				}
				
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			if (rs != null) {
				rs.close();
			}
			if (pstmt != null) {
				pstmt.close();
			}
			jdbc.closeSession();
		}
		
		
	}
	
	public void cadTabPrecoGar(ContextoAcao ctx, String referencia, BigDecimal valor) throws Exception{
		
		
		String key = preferenciaSenha();
		String ur = "http://api2.telecontrol.com.br/posvenda-core/tabelaPreco";

	    JsonObject requestBody = new JsonObject();
	    
	    
	  	// Preparando a requisi��o
        URL obj = new URL(ur);
        HttpURLConnection http = (HttpURLConnection) obj.openConnection();
        
        http.setRequestMethod("POST");
        http.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
        http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        http.setRequestProperty("Access-Application-Key", key);
		http.setRequestProperty("Access-Env", "PRODUCTION");
        http.setDoOutput(true);
        
	    requestBody.addProperty("siglaTabela", "GAR");
	    requestBody.addProperty("pecaReferencia", referencia.toString());
	    requestBody.addProperty("preco", valor);
	    System.out.println("Body Json GAR: " + requestBody);

        // Enviando a requisição
        OutputStream os = http.getOutputStream();
        os.write(requestBody.toString().getBytes("UTF-8"));
        os.flush();
        os.close();

        // Recebendo a resposta
        int responseCode = http.getResponseCode();
        
        if(responseCode == 400){
        	putTabPreco("http://api2.telecontrol.com.br/posvenda-core/tabelaPreco/siglaTabela/GAR/pecaReferencia/" + referencia, valor);
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
        }
	
	}
	
	public void cadTabPrecoVen(ContextoAcao ctx, String referencia, BigDecimal valor) throws Exception{

		String key = preferenciaSenha();
		String ur = "http://api2.telecontrol.com.br/posvenda-core/tabelaPreco";
		
		JsonObject requestBody = new JsonObject();
		
		// Preparando a requisi��o
		URL obj = new URL(ur);
		HttpURLConnection http = (HttpURLConnection) obj.openConnection();
		
		http.setRequestMethod("POST");
		http.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
		http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
		http.setRequestProperty("Access-Application-Key", key);
		http.setRequestProperty("Access-Env", "PRODUCTION");
		http.setDoOutput(true);

		requestBody.addProperty("siglaTabela", "VEN");
		requestBody.addProperty("pecaReferencia", referencia.toString());
		requestBody.addProperty("preco", valor);
		
		System.out.println("Body Json VEN: " + requestBody);
		
		// Enviando a requisi��o
		OutputStream os = http.getOutputStream();
		os.write(requestBody.toString().getBytes("UTF-8"));
		os.flush();
		os.close();
		
		// Recebendo a resposta
		int responseCode = http.getResponseCode();
		System.out.println("Response Code : " + responseCode);
		
		if(responseCode == 400){
        	putTabPreco("http://api2.telecontrol.com.br/posvenda-core/tabelaPreco/siglaTabela/VEN/pecaReferencia/" + referencia, valor);
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
        }
		
		
		
	}
	
public BigDecimal getVlrUnit(BigDecimal codprod, BigDecimal codtab) throws Exception{
		
		EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = entityFacade.getJdbcWrapper();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		BigDecimal vlrUnit = null;
		
		jdbc.openSession();
		
		String query = "SELECT CONVERT(NVARCHAR(50), FORMAT(COALESCE(EXC.VLRVENDA, 0), '0.00')) AS VLRVENDA "
					+"	FROM TGFTAB TAB "
					+"			INNER JOIN TGFEXC EXC ON EXC.NUTAB = TAB.NUTAB   "
					+"			INNER JOIN TGFNTA NTA ON NTA.CODTAB = TAB.CODTAB "
					+"	WHERE CODPROD = ? AND TAB.CODTAB = ? "
					+"	UNION ALL "
					+"	SELECT CONVERT(NVARCHAR(50), FORMAT(0, '0.00')) AS VLRVENDA "
					+"	WHERE NOT EXISTS ( "
					+"				SELECT 1 "
					+"				FROM TGFTAB TAB "
					+"				INNER JOIN TGFEXC EXC ON EXC.NUTAB = TAB.NUTAB   "
					+"				INNER JOIN TGFNTA NTA ON NTA.CODTAB = TAB.CODTAB "
					+"				WHERE CODPROD = ? AND TAB.CODTAB = ? "
					+"			)" ;
		
		pstmt = jdbc.getPreparedStatement(query);
		pstmt.setBigDecimal(1, codprod);
		pstmt.setBigDecimal(2, codtab);
		pstmt.setBigDecimal(3, codprod);
		pstmt.setBigDecimal(4, codtab);
		
		rs = pstmt.executeQuery();
		
		while(rs.next()){
			
			vlrUnit = new BigDecimal(rs.getString("VLRVENDA").replace(".", "").replace(",", "."));
			
		}
		
		if (rs != null) {
			rs.close();
		}
		if (pstmt != null) {
			pstmt.close();
		}
		jdbc.closeSession();
		
		
		return vlrUnit;
	}
	
public static String preferenciaSenha() throws Exception {
	
	EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();
	JdbcWrapper jdbc = entityFacade.getJdbcWrapper();
	PreparedStatement pstmt = null;
	String senha = null;
	ResultSet rs = null;
	
	try{
		
		jdbc.openSession();

		String sqlSenha = "SELECT VALOR FROM SANKHYA.AD_PARAMETROS WHERE "
				+ "PARAMETRO = 'chave api telecontrol'";
		pstmt = jdbc.getPreparedStatement(sqlSenha);

		rs = pstmt.executeQuery();
		while (rs.next()) {

			senha = rs.getString("VALOR");

		}

		System.out.println(senha);
		
	}catch (Exception e) {
		e.printStackTrace();
	}finally{
		if (rs != null) {
			rs.close();
		}
		if (pstmt != null) {
			pstmt.close();
		}
		jdbc.closeSession();
	}

	return senha;

}

public Map<String, String> obterParametro() throws Exception {
	EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();
	JdbcWrapper jdbc = entityFacade.getJdbcWrapper();
    PreparedStatement pstmt = null;
    ResultSet rs = null; 
    
    Map<String, String> parametro = null;
    
    try {
        jdbc.openSession();
        
        String sqlP = "SELECT\r\n"
        		+ "MAX(CASE WHEN PARAMETRO = 'Centro de Resultado para Telecontrol Garantia' THEN VALOR END) AS 'CenResultGar', \r\n"
        		+ "MAX(CASE WHEN PARAMETRO = 'Centro de Resultado para Telecontrol Venda' THEN VALOR END) AS 'CenResultVen', \r\n"
        		+ "MAX(CASE WHEN PARAMETRO = 'CIF/FOB para Telecontrol Garantia' THEN VALOR END) AS 'CIF/FOB Gar', \r\n"
        		+ "MAX(CASE WHEN PARAMETRO = 'CIF/FOB para Telecontrol Venda' THEN VALOR END) AS 'CIF/FOB Ven', \r\n"
        		+ "MAX(CASE WHEN PARAMETRO = 'Cod Projeto para Telecontrol' THEN VALOR END) AS 'Cod Projeto', \r\n"
        		+ "MAX(CASE WHEN PARAMETRO = 'Cod Transportadora para Telecontrol' THEN VALOR END) AS 'Cod Transportadora', \r\n"
        		+ "MAX(CASE WHEN PARAMETRO = 'Empresa do Telecontrol' THEN VALOR END) AS 'Empresa', \r\n"
        		+ "MAX(CASE WHEN PARAMETRO = 'Local de Estoque OS Telecontrol' THEN VALOR END) AS 'Local de Estoque', \r\n"
        		+ "MAX(CASE WHEN PARAMETRO = 'Natureza para OS Telecontrol Garantia' THEN VALOR END) AS 'Natureza Garantia',\r\n"
        		+ "MAX(CASE WHEN PARAMETRO = 'Natureza para OS Telecontrol Venda' THEN VALOR END) AS 'Natureza Venda', \r\n"
        		+ "MAX(CASE WHEN PARAMETRO = 'Tabela de preço para OS Telecontrol' THEN VALOR END) AS 'Tabela de preço', \r\n"
        		+ "MAX(CASE WHEN PARAMETRO = 'Tipo de Negociação para OS Telecontrol Garantia' THEN VALOR END) AS 'Tipo de Negociaçãoo Garantia', \r\n"
        		+ "MAX(CASE WHEN PARAMETRO = 'Tipo de Negociação para OS Telecontrol Venda' THEN VALOR END) AS 'Tipo de Negociação Venda', \r\n"
        		+ "MAX(CASE WHEN PARAMETRO = 'TOP para OS Telecontrol Garantia' THEN VALOR END) AS 'TOP para Garantia', \r\n"
        		+ "MAX(CASE WHEN PARAMETRO = 'TOP para OS Telecontrol Venda' THEN VALOR END) AS 'TOP para Venda' \r\n"
        		+ "FROM sankhya.AD_PARAMETROS \r\n"
        		+ "WHERE PARAMETRO LIKE '%Telecontrol%'";
        
        pstmt = jdbc.getPreparedStatement(sqlP);
        
        rs = pstmt.executeQuery();
        
        if (rs.next()) {
        	parametro = new HashMap<>();
        	parametro.put("cenReGar", rs.getString("CenResultGar"));
        	parametro.put("cenReVen", rs.getString("CenResultVen"));
        	parametro.put("cifFobGar", rs.getString("CIF/FOB Gar"));
        	parametro.put("cifFobVen", rs.getString("CIF/FOB Ven"));
        	parametro.put("codProjeto", rs.getString("Cod Projeto"));
        	parametro.put("codTransport", rs.getString("Cod Transportadora"));
        	parametro.put("codEmp", rs.getString("Empresa"));
        	parametro.put("locEst", rs.getString("Local de Estoque"));
        	parametro.put("natGar", rs.getString("Natureza Garantia"));
        	parametro.put("natVen", rs.getString("Natureza Venda"));
        	parametro.put("tabPreco", rs.getString("Tabela de preço"));
        	parametro.put("tipNegGar", rs.getString("Tipo de Negociação Garantia"));
        	parametro.put("tipNegVen", rs.getString("Tipo de Negociação Venda"));
        	parametro.put("topGar", rs.getString("TOP para Garantia"));
        	parametro.put("topVen", rs.getString("TOP para Venda"));
        }
    } catch (SQLException e) {
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
    
    return parametro;
}

public void putTabPreco(String ur, BigDecimal preco) throws Exception {
	
	String key = preferenciaSenha();

    // Preparando a requisi��o
    URL obj = new URL(ur);
    HttpURLConnection http = (HttpURLConnection) obj.openConnection();
    
    http.setRequestMethod("PUT");
    http.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
    http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
    http.setRequestProperty("Access-Application-Key", key);
	http.setRequestProperty("Access-Env", "PRODUCTION");
    http.setDoOutput(true);
    
    JsonObject requestBody = new JsonObject();
    
    requestBody.addProperty("preco", preco);
    
    // Enviando a requisi��o
    OutputStream os = http.getOutputStream();
    os.write(requestBody.toString().getBytes("UTF-8"));
    os.flush();
    os.close();

    // Recebendo a resposta
    int responseCode = http.getResponseCode();
    System.out.println("Response Code : " + responseCode);

    BufferedReader in = new BufferedReader(new InputStreamReader(http.getInputStream()));
    StringBuffer response = new StringBuffer();
    
    String inputLine;
    while ((inputLine = in.readLine()) != null) {
        response.append(inputLine);
    }
    
    in.close();

    // Exibindo a resposta
    System.out.println(response.toString());
		
}

}
