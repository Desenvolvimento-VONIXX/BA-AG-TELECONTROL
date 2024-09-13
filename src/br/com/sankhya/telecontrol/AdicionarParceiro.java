package br.com.sankhya.telecontrol;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;


import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.core.JapeSession.SessionHandle;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

import org.cuckoo.core.ScheduledAction;
import org.cuckoo.core.ScheduledActionContext;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.sankhya.util.JsonUtils;


public class AdicionarParceiro implements AcaoRotinaJava  {
	
	//SELECT * FROM AD_PECAPRODUTOTELECONTROL WHERE CODPROD = 2011029 AND CODPECA = 2005081
	@Override
	public void doAction(ContextoAcao ctx) throws Exception {
		try{
			apiPostFamilia("http://api2.telecontrol.com.br/posvenda-core/familias");

		} catch (Exception e) {
			e.printStackTrace();
		} 
		
		try{
			apiPostPecas("http://api2.telecontrol.com.br/posvenda-core/peca");
			
		} catch (Exception e) {
			e.printStackTrace();
		} 
		
		try{
			apiPostProdMaq("http://api2.telecontrol.com.br/posvenda-core/produtos");
		
		} catch (Exception e) {
			e.printStackTrace();
		} 
		
		try{
			apiPostAssist("http://api2.telecontrol.com.br/posvenda-core/posto/");					
		
		} catch (Exception e) {
			e.printStackTrace();
		} 
		
		try{
			apiPostListaBasica("http://api2.telecontrol.com.br/posvenda-core/listaBasica");					
			
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
	
public static void apiPostProdMaq(String ur) throws Exception {
		
	    EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = entityFacade.getJdbcWrapper();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		StringBuffer mensagem = new StringBuffer();
		String key = preferenciaSenha();
		
		String descrProd;
        String referencia;
        String ipi;
        String origProd;
        boolean ativo;
        String valorAssist;
        String linha = null;
        String grupoFam;
        String garantia;
        String def;
        
		jdbc.openSession();
	
	 // Fazendo o select na tabela
	    String sql = "SELECT PRO.CODPROD, RTRIM(PRO.DESCRPROD) AS DESCRPROD, "
	    		+ "PRO.REFERENCIA, PRO.ORIGPROD, PRO.ATIVO, format(IPI.PERCENTUAL, '0.00') as IPI, "
	    		+ "format(AD_VALORASSISTENCIATELECONTROL, '0.00') as AD_VALORASSISTENCIATELECONTROL, "
	    		+ "PRO.AD_LINHASTELECONTROL, PRO.CODGRUPOPROD, PRO.AD_TEMPOGARANTIATELECONTROL "
	    		+ "FROM TGFPRO AS PRO "
	    		+ "INNER JOIN TGFIPI IPI ON IPI.CODIPI = PRO.CODIPI "
	    		+ "WHERE PRO.ATIVO = 'S' "
	    		+ "AND PRO.AD_PRODUTOTELECONTROL = 1 "
	    		+ "AND (PRO.AD_ENVIADOTELECONTROL <> 1 OR PRO.AD_ENVIADOTELECONTROL IS NULL)";
	    
	    pstmt = jdbc.getPreparedStatement(sql);
	    rs = pstmt.executeQuery();
	
	    // Percorrendo o ResultSet e fazendo o POST
	    while (rs.next()) {
	        descrProd = rs.getString("DESCRPROD");
	        referencia = rs.getString("REFERENCIA");
	        origProd = rs.getInt("ORIGPROD") == 0 ? "NAC" : "IMP";
	        ativo = rs.getString("ATIVO").equalsIgnoreCase("S") ? true : false;
	        ipi = rs.getString("IPI").replace("," , ".");
	        valorAssist = rs.getString("AD_VALORASSISTENCIATELECONTROL").replace("," , ".");
	        def = rs.getString("AD_LINHASTELECONTROL");
	        
	        if(def.equalsIgnoreCase("ELETRICA")){
	        	linha = "0001";
	        }else if(def.equalsIgnoreCase("HIDRAULICA")){
	        	linha = "0003";
	        }else if(def.equalsIgnoreCase("MECANICA")){
	        	linha = "0002";
	        }
	        
	        grupoFam = Integer.toString(rs.getInt("CODGRUPOPROD"));
	        garantia = Integer.toString(rs.getInt("AD_TEMPOGARANTIATELECONTROL"));
	        
	
	        // Preparando a requisi��o
	        URL obj = new URL(ur);
	        HttpURLConnection http = (HttpURLConnection) obj.openConnection();
	        
	        http.setRequestMethod("POST");
	        http.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
	        http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
	        http.setRequestProperty("Access-Application-Key", key);
			http.setRequestProperty("Access-Env", "PRODUCTION");
	        http.setDoOutput(true);
	
	        // Preparando o corpo da requisi��o
	        
	        JsonObject produto = new JsonObject();
			
	        produto.addProperty("descricao", descrProd);
	        produto.addProperty("referencia", referencia);
	        produto.addProperty("codigoFamilia", grupoFam);
	        produto.addProperty("codigoLinha", linha);
	        produto.addProperty("garantia", garantia);
	        produto.addProperty("maoDeObra", valorAssist);
	        produto.addProperty("maoDeObraAdmin", "0");
	        produto.addProperty("ativo", ativo);
	        produto.addProperty("origem", origProd);
	        produto.addProperty("ipi", ipi);
	        
	        System.out.println(produto);
	        
	        // Enviando a requisi��o
	        OutputStream os = http.getOutputStream();
	        os.write(produto.toString().getBytes("UTF-8"));
	        os.flush();
	        os.close();
	
	        // Recebendo a resposta
	        int responseCode = http.getResponseCode();
	        System.out.println("Response Code : " + responseCode);
	
	        BufferedReader in = new BufferedReader(new InputStreamReader(http.getInputStream()));
	        String inputLine;
	        StringBuffer response = new StringBuffer();
	
	        while ((inputLine = in.readLine()) != null) {
	            response.append(inputLine);
	        }
	        
	        if(responseCode == 201){
	        	int codProd = rs.getInt("CODPROD");
	        	
	        	update(codProd, "TGFPRO", "AD_ENVIADOTELECONTROL", "CODPROD");
	        }
	        
	        in.close();
	
	        // Exibindo a resposta
	        System.out.println(response.toString());
	    }
	        jdbc.closeSession();		
	}
	
	public static void apiPostListaBasica(String ur) throws Exception {
		
		EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = entityFacade.getJdbcWrapper();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		StringBuffer mensagem = new StringBuffer();
		String key = preferenciaSenha();
		
		String codprod = null;
		String codpecas = null;
		String posicao = null;
		BigDecimal quantidade = BigDecimal.ZERO;
		BigDecimal codProd = BigDecimal.ZERO;
		
		jdbc.openSession();
		
		// Fazendo o select na tabela
		String sql = "SELECT CODPROD, INTERACAO, CODPECA, QTDPECAS FROM "
				+ "AD_PECAPRODUTOTELECONTROL WHERE ENVIADOTELECONTROL <> 1 OR ENVIADOTELECONTROL IS NULL";
		
		pstmt = jdbc.getPreparedStatement(sql);
		rs = pstmt.executeQuery();
		
		// Percorrendo o ResultSet e fazendo o POST
		while (rs.next()) {
			
			codProd = rs.getBigDecimal("CODPROD");
			codprod = getReferencia(new BigDecimal(rs.getString("CODPROD")));
			codpecas = getReferencia(new BigDecimal(rs.getString("CODPECA")));
			posicao = rs.getString("INTERACAO");
			quantidade = rs.getBigDecimal("QTDPECAS");
			
			if(codprod != null && codpecas != null){
				// Preparando a requisi��o
				URL obj = new URL(ur);
				HttpURLConnection http = (HttpURLConnection) obj.openConnection();
				
				http.setRequestMethod("POST");
				http.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
				http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
				http.setRequestProperty("Access-Application-Key", key);
				http.setRequestProperty("Access-Env", "PRODUCTION");
				http.setDoOutput(true);
				
				// Preparando o corpo da requisi��o
				
				JsonObject produto = new JsonObject();
				
				produto.addProperty("referenciaProduto", codprod);
				produto.addProperty("referenciaPeca", codpecas);
				produto.addProperty("quantidade", quantidade);
				produto.addProperty("posicao", posicao);
				
				System.out.println(produto);
				
				// Enviando a requisi��o
				OutputStream os = http.getOutputStream();
				os.write(produto.toString().getBytes("UTF-8"));
				os.flush();
				os.close();
				
				if(http.getResponseCode() != 201){
					System.out.println("Erro de cadastro");
				}else{
					// Recebendo a resposta
					int responseCode = http.getResponseCode();
					System.out.println("Response Code : " + responseCode);
					
					BufferedReader in = new BufferedReader(new InputStreamReader(http.getInputStream()));
					String inputLine;
					StringBuffer response = new StringBuffer();
					
					while ((inputLine = in.readLine()) != null) {
						response.append(inputLine);
					}
					
					if(responseCode == 201){
						int codpeca = rs.getInt("CODPECA");
						
						updateTabBasica(codProd, codpeca, "ENVIADOTELECONTROL", posicao);
					}
					
					in.close();
					
					// Exibindo a resposta
					System.out.println(response.toString());
				}
				
			}else{
				System.out.println("Valores nulos: " + codprod + " \\ " + codpecas);
			}
			
		}
		jdbc.closeSession();		
	}

	
	public static void apiPostAssist(String ur) throws Exception{
		
		EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = entityFacade.getJdbcWrapper();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		StringBuffer mensagem = new StringBuffer();
		String key = preferenciaSenha();
		
		
		String razao;
		String nomeFantasia;
		String cnpj;
		String ie;
		String codigoPosto;
		String endereco;
		int numero;
		String bairro;
		String cep;
		String cidade;
		String estado;
		String email;
		String fone;
		String credenciamento;
		
		jdbc.openSession();
		
	 // Fazendo o select na tabela
	    String sql = "SELECT PAR.CODPARC, \r\n"
	    		+ "RTRIM(PAR.RAZAOSOCIAL) AS RAZAOSOCIAL,\r\n"
	    		+ "RTRIM(PAR.NOMEPARC) AS NOMEPARC, \r\n"
	    		+ "PAR.CGC_CPF, \r\n"
	    		+ "RTRIM(PAR.IDENTINSCESTAD) AS IDENTINSCESTAD, \r\n"
	    		+ "EN.NOMEEND, PAR.CEP, \r\n"
	    		+ "RTRIM(PAR.NUMEND) AS NUMEND, BAI.NOMEBAI, \r\n"
	    		+ "CID.NOMECID, UFS.UF, \r\n"
	    		+ "RTRIM(PAR.EMAIL) AS EMAIL, PAR.TELEFONE, \r\n"
	    		+ "PAR.AD_CREDENCIADO_TELECONTROL,\r\n"
	    		+ "AD_PARCEIROTELECONTROL,\r\n"
	    		+ "AD_ENVIADOTELECONTROL\r\n"
	    		+ "FROM SANKHYA.TGFPAR AS PAR \r\n"
	    		+ "INNER JOIN SANKHYA.TSIEND EN ON EN.CODEND = PAR.CODEND \r\n"
	    		+ "INNER JOIN SANKHYA.TSIBAI BAI ON BAI.CODBAI = PAR.CODBAI \r\n"
	    		+ "INNER JOIN SANKHYA.TSICID CID ON CID.CODCID = PAR.CODCID \r\n"
	    		+ "INNER JOIN SANKHYA.TSIUFS UFS ON UFS.CODUF = CID.UF \r\n"
	    		+ "WHERE \r\n"
	    		+ "PAR.AD_PARCEIROTELECONTROL = 1 \r\n"
	    		+ "AND (AD_ENVIADOTELECONTROL <> '1' or AD_ENVIADOTELECONTROL is null) \r\n"
	    		+ "AND ATIVO = 'S'\r\n"
	    		+ "AND CODPARC NOT IN (24156)";
	    
	    pstmt = jdbc.getPreparedStatement(sql);
	    rs = pstmt.executeQuery();
	    
	    // Percorrendo o ResultSet e fazendo o POST
	    
		while (rs.next()) {
			
			razao = rs.getString("RAZAOSOCIAL").trim();
			nomeFantasia = rs.getString("NOMEPARC").trim();
			cnpj = rs.getString("CGC_CPF").trim();
			ie = rs.getString("IDENTINSCESTAD").trim();
			codigoPosto = Integer.toString(rs.getInt("CODPARC"));
			endereco = rs.getString("NOMEEND").trim();
			if(rs.getString("NUMEND") == null){
				numero = 0;
			    }else{
			    	numero = Integer.parseInt(rs.getString("NUMEND"));
			    }
			bairro = rs.getString("NOMEBAI").trim();
			cep = rs.getString("CEP").trim();
			cidade = rs.getString("NOMECID").trim();
			estado = rs.getString("UF").trim();
			email = rs.getString("EMAIL").trim();
			fone = rs.getString("TELEFONE").trim();
			credenciamento = rs.getInt("AD_CREDENCIADO_TELECONTROL") == 1 ? "CREDENCIADO" : "DESCREDENCIADO";
			
			
		    // Preparando a requisi��o
		    URL obj = new URL(ur);
		    HttpURLConnection http = (HttpURLConnection) obj.openConnection();
	
		    http.setRequestMethod("POST");
		    http.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
		    http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
		    http.setRequestProperty("Access-Application-Key", key);
			http.setRequestProperty("Access-Env", "PRODUCTION");
		    http.setDoOutput(true);
	
		    // Preparando o corpo da requisi��o
		    JsonObject parceiro = new JsonObject();
			
			parceiro.addProperty("razao", razao);
			parceiro.addProperty("nomeFantasia", nomeFantasia);
			parceiro.addProperty("cnpj", cnpj);
			parceiro.addProperty("ie", ie);
			parceiro.addProperty("codigoPosto", codigoPosto);
			parceiro.addProperty("endereco", endereco);
			parceiro.addProperty("numero", numero);
			parceiro.addProperty("bairro", bairro);
			parceiro.addProperty("cep", cep);
			parceiro.addProperty("cidade", cidade);
			parceiro.addProperty("estado", estado);
			parceiro.addProperty("email", email);
			parceiro.addProperty("fone", fone);
			parceiro.addProperty("tipoPosto", "Aut");
			parceiro.addProperty("credenciamento", credenciamento);
			
			System.out.println("Teste Parceiro: " + parceiro);
		    
		    // Enviando a requisi��o
		    OutputStream os = http.getOutputStream();
		    os.write(parceiro.toString().getBytes("UTF-8"));
		    os.flush();
		    os.close();
	
		    // Recebendo a resposta
		    int responseCode = http.getResponseCode();
		    System.out.println("Response Code : " + responseCode);
		    
		    if(responseCode != 400 && responseCode != 404){
		    	BufferedReader in = new BufferedReader(new InputStreamReader(http.getInputStream()));
			    String inputLine;
			    StringBuffer response = new StringBuffer();
		
			    while ((inputLine = in.readLine()) != null) {
			        response.append(inputLine);
			    }
			    
			    //atualizando o que ja foi enviado
			    if(responseCode == 201){
			    	int codparc = rs.getInt("CODPARC");
			    	
			    	update(codparc, "TGFPAR", "AD_ENVIADOTELECONTROL", "CODPARC");
			       
			    }
			    
			    in.close();
			    
			    // Exibindo a resposta
			    System.out.println(response.toString());
			    
		    }else if(responseCode == 400){
		    	int codparc = rs.getInt("CODPARC");
		    	
		    	update(codparc, "TGFPAR", "AD_ENVIADOTELECONTROL", "CODPARC");
		    }
		    
		}
	    
	    jdbc.closeSession();		
	}

	public static void apiPostPecas(String ur) throws Exception {
		
	    EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = entityFacade.getJdbcWrapper();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		StringBuffer mensagem = new StringBuffer();
		String key = preferenciaSenha();
		
		String descrProd;
        String referencia;
        String ipi;
        String origProd;
        boolean ativo;
        boolean acessorioTelecontrol;
        boolean bloqueadaVenda;
        boolean bloqueadaGarantia;
        String codvol;
        Double estoque;
        
        int qtdEmb = 0;
        
		jdbc.openSession();
	
	 // Fazendo o select na tabela
	    String sql = "SELECT PRO.CODPROD, PRO.QTDEMB, RTRIM(PRO.DESCRPROD) AS DESCRPROD, PRO.REFERENCIA, PRO.ORIGPROD, PRO.ATIVO, "
	    		+ "PRO.AD_BLOQUEADOVENDATELECONTROL, PRO.AD_BLOQUEADOGARANTIATELECONTROL, "
	    		+ "PRO.AD_ACESSORIOTELECONTROL, PRO.CODVOL, format(IPI.PERCENTUAL, '0.00') as IPI, "
	    		+ "format(EST.ESTOQUE, '0.00') as ESTOQUE "
	    		+ "FROM TGFPRO AS PRO "
	    		+ "INNER JOIN TGFIPI IPI ON IPI.CODIPI = PRO.CODIPI "
	    		+ "INNER JOIN TGFEST EST ON EST.CODPROD = PRO.CODPROD "
	    		+ "WHERE EST.CODLOCAL = 1030000 AND AD_ACESSORIOTELECONTROL = 1 "
	    		+ "AND (AD_ENVIADOTELECONTROL <> 1 OR AD_ENVIADOTELECONTROL IS NULL) ";
	    
	    pstmt = jdbc.getPreparedStatement(sql);
	    rs = pstmt.executeQuery();
	
	    // Percorrendo o ResultSet e fazendo o POST
	    while (rs.next()) {
	        descrProd = rs.getString("DESCRPROD");
	        referencia = rs.getString("REFERENCIA");
	        origProd = rs.getString("ORIGPROD").equals("0") ? "NAC" : "IMP";
	        ativo = rs.getString("ATIVO").equalsIgnoreCase("S") ? true : false;
	        acessorioTelecontrol = true;
	        codvol = rs.getString("CODVOL");
	        ipi = rs.getString("IPI").replace("," , ".");
	        estoque = Double.parseDouble(rs.getString("ESTOQUE").replace("," , "."));
	        qtdEmb = rs.getInt("QTDEMB");
	        
	        if(rs.getString("AD_BLOQUEADOVENDATELECONTROL") == null){
	        	bloqueadaVenda = false;
	        }else{
		        bloqueadaVenda = rs.getString("AD_BLOQUEADOVENDATELECONTROL").equals("1") ? true : false;
	        }
	        
	        if(rs.getString("AD_BLOQUEADOGARANTIATELECONTROL") == null){
	        	bloqueadaGarantia = false;
	        }else{
	        	bloqueadaGarantia = rs.getString("AD_BLOQUEADOGARANTIATELECONTROL").equals("1") ? true : false;
	        }
	
	        // Preparando a requisi��o
	        URL obj = new URL(ur);
	        HttpURLConnection http = (HttpURLConnection) obj.openConnection();
	        
	        http.setRequestMethod("POST");
	        http.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
	        http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
	        http.setRequestProperty("Access-Application-Key", key);
			http.setRequestProperty("Access-Env", "PRODUCTION");
	        http.setDoOutput(true);
	
	        // Preparando o corpo da requisi��o
	        
	        JsonObject pecas = new JsonObject();
			
	        pecas.addProperty("descricao", descrProd);
	        pecas.addProperty("referencia", referencia);
	        pecas.addProperty("ipi", ipi);
	        pecas.addProperty("numeroSeriePeca", true);
	        pecas.addProperty("acessorio", acessorioTelecontrol);
	        pecas.addProperty("itemAparencia", false);
	        pecas.addProperty("origem", origProd);
	        pecas.addProperty("ativo", ativo);
	        pecas.addProperty("bloqueadaVenda", bloqueadaVenda);
	        pecas.addProperty("bloqueadaGarantia", bloqueadaGarantia);
	        pecas.addProperty("estoque", estoque);
	        pecas.addProperty("unidade", codvol);
	        pecas.addProperty("multiplo", qtdEmb);
	        
	        System.out.println(pecas);
	        
	        // Enviando a requisi��o
	        OutputStream os = http.getOutputStream();
	        os.write(pecas.toString().getBytes("UTF-8"));
	        os.flush();
	        os.close();
	
	        // Recebendo a resposta
	        int responseCode = http.getResponseCode();
	        System.out.println("Response Code : " + responseCode);
	
	        BufferedReader in = new BufferedReader(new InputStreamReader(http.getInputStream()));
	        String inputLine;
	        StringBuffer response = new StringBuffer();
	
	        while ((inputLine = in.readLine()) != null) {
	            response.append(inputLine);
	        }
	        
	        if(responseCode == 201){
	        	int codProd = rs.getInt("CODPROD");
	        	
	        	update(codProd, "TGFPRO", "AD_ENVIADOTELECONTROL", "CODPROD");
	        }
	        
	        in.close();
	
	        // Exibindo a resposta
	        System.out.println(response.toString());
	    }
	        jdbc.closeSession();		
	}
	
	public static void update(int codpk, String tabela, String campo, String cond){
		
		EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = entityFacade.getJdbcWrapper();
		PreparedStatement pstmt = null;
		
		try {
			jdbc.openSession();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		
		String sqlUpdate = "UPDATE " +tabela+ " SET " +campo+ " = '1' WHERE " + cond + " = " + codpk;
		
        try {
			pstmt = jdbc.getPreparedStatement(sqlUpdate);
			pstmt.executeUpdate();
			
		} catch (Exception e) {
			
			e.printStackTrace();
		}finally{
	        jdbc.closeSession();
		}
	}
	
	public static void updateTabBasica(BigDecimal codprod, int codpeca, String campo, String interacao){
		
		EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = entityFacade.getJdbcWrapper();
		PreparedStatement pstmt = null;
		
		try {
			jdbc.openSession();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		
		String sqlUpdate = "UPDATE AD_PECAPRODUTOTELECONTROL SET " +campo+ " = '1' WHERE CODPROD = " + codprod + " AND INTERACAO = " + interacao + " AND CODPECA = " + codpeca;
		
		try {
			pstmt = jdbc.getPreparedStatement(sqlUpdate);
			pstmt.executeUpdate();
			
		} catch (Exception e) {
			
			e.printStackTrace();
		}finally{
			jdbc.closeSession();
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
	
	public static String getReferencia(BigDecimal codprod) throws Exception {
		
		EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = entityFacade.getJdbcWrapper();
		PreparedStatement pstmt = null;
		String referencia = null;
		ResultSet rs = null;
		
		jdbc.openSession();
		
		String sqlSenha = "SELECT REFERENCIA FROM TGFPRO WHERE CODPROD = ?";
		pstmt = jdbc.getPreparedStatement(sqlSenha);
		pstmt.setBigDecimal(1, codprod);
		
		rs = pstmt.executeQuery();
		
		while (rs.next()) {
			
			referencia = rs.getString("REFERENCIA");
			
		}
		
		System.out.println(referencia);
		
		jdbc.closeSession();
		
		return referencia;
		
	}
	
	
	public static void apiPostFamilia(String ur) throws Exception {
		
	    EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = entityFacade.getJdbcWrapper();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		StringBuffer mensagem = new StringBuffer();
		String key = preferenciaSenha();
		
		String descrgru;
        String codgru;
        
		jdbc.openSession();
		
	
	 // Fazendo o select na tabela
	    String sql = "SELECT CODGRUPOPROD, "
	    		+ "RTRIM(DESCRGRUPOPROD) as DESCRGRUPOPROD "
	    		+ "FROM sankhya.tgfgru WHERE ATIVO = 'S' "
	    		+ "AND (AD_ENVIADOTELECONTROL <> 1 OR AD_ENVIADOTELECONTROL IS NULL) "
	    		+ "AND AD_GRUPOTELECONTROL = 1";
	    
	    pstmt = jdbc.getPreparedStatement(sql);
	    rs = pstmt.executeQuery();
	    
	
	    // Percorrendo o ResultSet e fazendo o POST
	    while (rs.next()) {
	    	descrgru = rs.getString("DESCRGRUPOPROD");
	    	codgru = Integer.toString(rs.getInt("CODGRUPOPROD"));
	
	    	// Preparando a requisi��o
	    	URL obj = new URL(ur);
	    	HttpURLConnection http = (HttpURLConnection) obj.openConnection();

	    	http.setRequestMethod("POST");
	    	http.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
	    	http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
	    	http.setRequestProperty("Access-Application-Key", key);
	    	http.setRequestProperty("Access-Env", "PRODUCTION");
	    	http.setDoOutput(true);

	    	// Preparando o corpo da requisi��o
	    	JsonObject requestBody = new JsonObject();

	    	requestBody.addProperty("fatherUuid", "string");
	    	requestBody.addProperty("nome", "string");
	    	requestBody.addProperty("descricao", "string");
	    	requestBody.addProperty("peso", 0);
	    	requestBody.addProperty("duration", 0);
	    	requestBody.addProperty("responsavelId", 0);
	    	requestBody.addProperty("agencyId", 0);
	    	requestBody.addProperty("inicioPrevisto", "string");
	    	requestBody.addProperty("terminoPrevisto", "string");
	    	requestBody.addProperty("inicioRealizado", "string");
	    	requestBody.addProperty("terminoRealizado", "string");
	    	requestBody.addProperty("baselineStart", "string");
	    	requestBody.addProperty("baselineFinish", "string");
	    	requestBody.addProperty("endTrend", "string");

            JsonArray tagIds = new JsonArray();
            tagIds.add(new JsonPrimitive(0));
            requestBody.add("tagIds", tagIds);

            JsonArray dynamicFieldList = new JsonArray();
            JsonObject dynamicField = new JsonObject();
            dynamicField.addProperty("dynamicFieldId", 0);
            
            JsonArray dynamicFieldValue = new JsonArray();
            dynamicFieldValue.add(new JsonPrimitive("string"));
            dynamicField.add("value", dynamicFieldValue);

            dynamicFieldList.add(dynamicField);
            requestBody.add("dynamicFieldList", dynamicFieldList);

            System.out.println(requestBody.toString());

	    	// Enviando a requisi��o
	    	OutputStream os = http.getOutputStream();
	    	os.write(new Gson().toJson(requestBody).getBytes("UTF-8"));
	    	os.flush();
	    	os.close();
	
	        // Recebendo a resposta
	        int responseCode = http.getResponseCode();
	        System.out.println("Response Code : " + responseCode);
	
	        BufferedReader in = new BufferedReader(new InputStreamReader(http.getInputStream()));
	        String inputLine;
	        StringBuffer response = new StringBuffer();
	
	        while ((inputLine = in.readLine()) != null) {
	            response.append(inputLine);
	        }
	        
	        if(responseCode == 201){
	        	int codgruF = rs.getInt("CODGRUPOPROD");
	        	
	        	update(codgruF, "TGFGRU", "AD_ENVIADOTELECONTROL", "CODGRUPOPROD");
	        }
	        
	        in.close();
	
	        // Exibindo a resposta
	        System.out.println(response.toString());
	    }
	        jdbc.closeSession();		
	}


}

