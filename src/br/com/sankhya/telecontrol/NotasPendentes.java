package br.com.sankhya.telecontrol;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.Timestamp;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sankhya.util.JdbcUtils;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.core.JapeSession.SessionHandle;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class NotasPendentes implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao ctx) throws Exception {
		// TODO Auto-generated method stub
		try {
			apiFaturamento(ctx);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public String[] api(String ur) throws Exception {

		BufferedReader reader;
		String line;
		StringBuilder responseContent = new StringBuilder();
		String key = preferenciaSenha();
		
		URL url = new URL(ur);
		

		HttpURLConnection http = (HttpURLConnection) url.openConnection();
		http.setConnectTimeout(10000);
		http.addRequestProperty("User-Agent",
				"Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
		http.setRequestProperty("Access-Application-Key", key);
		http.setRequestProperty("Access-Env", "PRODUCTION");
		http.setRequestProperty("Content-Type", "application/json");
		http.setDoOutput(true);
		http.setDoInput(true);
		
		int status = http.getResponseCode();
	
		if (status >= 300) {
			reader = new BufferedReader(new InputStreamReader(
					http.getErrorStream()));
			while ((line = reader.readLine()) != null) {
				responseContent.append(line);
			}
			reader.close();
		} else {
			reader = new BufferedReader(new InputStreamReader(
					http.getInputStream()));
			while ((line = reader.readLine()) != null) {
				responseContent.append(line);
			}
			reader.close();
		}
		System.out.println("Output from Server .... \n" + status);
		String response = responseContent.toString();
	
		http.disconnect();
		
		return new String[] {Integer.toString(status), response};
		
	}
	

	public void inserirTelaTelecon(BigDecimal codPedido, BigDecimal codparc, String cnpj, String data) throws Exception {
	    
		
		JapeSession.SessionHandle hnd = null;
	    JdbcWrapper jdbc = null;
	    NativeSql sql = null;
	    ResultSet rs = null;

	    try {
	        hnd = JapeSession.open(); // Abrindo a sessão Jape
	        jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();
	        jdbc.openSession(); // Abrindo a sessão JDBC

	        // Verificar se o codPedido já existe na tabela
	        sql = new NativeSql(jdbc);
	        sql.appendSql("SELECT COUNT(*) AS TOTAL FROM AD_NOTASPENDENTESTELECONTROL WHERE CODPEDIDO = :CODPEDIDO");
	        sql.setNamedParameter("CODPEDIDO", codPedido);

	        rs = sql.executeQuery();

	        if (rs.next() && rs.getInt("TOTAL") == 0) {
	      
	            
	            try {
	    			hnd = JapeSession.open();
	    			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();

	    			JapeWrapper cotDAO = JapeFactory.dao("AD_NOTASPENDENTESTELECONTROL");
	    			

	    			DynamicVO cotVo = cotDAO.create()
	    					.set("CODPEDIDO", codPedido)
		                    .set("CODPARC", codparc)
		                    .set("CNPJ", cnpj)
		                    .set("DATA", data)
		                    .save();

	    		} catch (Exception e) {
	    			throw new MGEModelException("Erro ao criar registro", e);
	    		} finally {
	    			JdbcUtils.closeResultSet(null);
	    			NativeSql.releaseResources(sql);
	    			JdbcWrapper.closeSession(jdbc);
	    			JapeSession.close(hnd);
	    		}
	        } else {
	            throw new MGEModelException("Registro com o mesmo CODPEDIDO já existe.");
	        }

	    } catch (Exception e) {
	        throw new MGEModelException("Erro ao criar registro", e);
	    } finally {
	        JdbcUtils.closeResultSet(rs); 
	        NativeSql.releaseResources(sql); 
	        JdbcWrapper.closeSession(jdbc);
	        JapeSession.close(hnd); 
	    }
	}
	
	public void updateStatus(String statusErro, BigDecimal codPedido) throws Exception {
		JapeSession.SessionHandle hnd = null;
	    JdbcWrapper jdbc = null;
	    NativeSql query = null;
	    	    
	    try {
	        String update = "UPDATE SANKHYA.AD_NOTASPENDENTESTELECONTROL SET STATUSERRO = :STATUSERRO WHERE CODPEDIDO = :CODPEDIDO";
	        hnd = JapeSession.open();
	        hnd.setCanTimeout(false);
	        hnd.setFindersMaxRows(-1);
	        EntityFacade entity = EntityFacadeFactory.getDWFFacade();
	        jdbc = entity.getJdbcWrapper();
	        jdbc.openSession();
	        query = new NativeSql(jdbc);
	        query.setNamedParameter("STATUSERRO", statusErro);
	        query.setNamedParameter("CODPEDIDO", codPedido );
	        query.appendSql(update);
	        query.executeUpdate();
	        
	    } catch (Exception e) {
	        e.printStackTrace();
	        throw new Exception("Erro ao executar a atualização: " + e.getMessage());
	    } finally {
	        JapeSession.close(hnd);
	        JdbcWrapper.closeSession(jdbc);
	        NativeSql.releaseResources(query);
	    }

	}

	
	public String preferenciaSenha() throws Exception {
			
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

	public void apiFaturamento(ContextoAcao ctx) throws Exception {
		
		// Obter a data atual
	    LocalDate currentDate = LocalDate.now();
	
	    // Subtrair 5 dias da data atual
	    LocalDate fiveDaysAgo = currentDate.minusDays(5);
	
	    // Formatando as datas
	    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	    String formattedCurrentDate = currentDate.format(formatter);
	    String formattedFiveDaysAgo = fiveDaysAgo.format(formatter);
	    
	    System.out.println("Data da API: " + formattedCurrentDate);
	    System.out.println("Data da API A 5 DIAS: " + formattedFiveDaysAgo);
		String[] result = api("http://api2.telecontrol.com.br/posvenda-pedido/pedidos/dataInicial/"+formattedFiveDaysAgo+"/dataFinal/"+formattedCurrentDate);
		
		try{
			
			SessionHandle hnd = JapeSession.open();
			
			System.out.println("Entrou aqui JOBFaturamento");
	
			//JapeWrapper parametrosExtDAO = JapeFactory
					//.dao("ParametrosExtensoes");
			//DynamicVO parametroVO;
			
				//parametroVO = parametrosExtDAO
						//.findByPK("DOMAIN_SERVICE");
			
			//parametroVO.asString("VALOR");
			String domain = "http://192.168.0.101:8180";
	
			JapeWrapper usuarioDAO = JapeFactory
					.dao(DynamicEntityNames.USUARIO);
			DynamicVO usuarioVO = usuarioDAO
					.findByPK(new BigDecimal(0));
			String md5 = usuarioVO.getProperty("INTERNO")
					.toString();
			String nomeUsu = usuarioVO.getProperty("NOMEUSU")
					.toString();
			
			
			//JapeSession.close(hnd);
		}catch (Exception e2) {
			e2.printStackTrace();
		}
		
		System.out.println("Instanciou A API");
		String response = result[1];
		BigDecimal nuNota = null;
				
		Map<String, String> parametro;
				
		parametro = obterParametro(); 

		String codEmp = parametro.get("codEmp");
		String top = "";
	    String codTipNeg = "";
	    String codcecus = "";
	    String natureza = "";
	    String cifFob = null;
		
		String dhTop = "";
		String tipMov = "";
		String dhTipNeg = "";
		String quantidade = "";
		String referencia = "";
		String nunota = null;
		String pedidoItem = null;
		
		BigDecimal qtdVol = BigDecimal.ZERO;
		BigDecimal pesoLiq = BigDecimal.ZERO;
		BigDecimal qtdNeg = BigDecimal.ZERO;
		BigDecimal pesoBruto = BigDecimal.ZERO;
		BigDecimal codprod = BigDecimal.ZERO;
		BigDecimal codprodVar = BigDecimal.ZERO;
		BigDecimal tabPreco = BigDecimal.ZERO;
		BigDecimal vlrUnit = BigDecimal.ZERO;
		BigDecimal vlrTotal = BigDecimal.ZERO;
		BigDecimal codlocal = BigDecimal.ZERO;
		BigDecimal ipi = BigDecimal.ZERO;
		BigDecimal topBg = null;
	    BigDecimal codTipNegBg = null;
	    BigDecimal codcecusBg = null;
	    BigDecimal naturezaBg = null;
	    BigDecimal tipMovBg = null;
	    BigDecimal codEmpBg = null;
	    
	    Map<String, String> produto;
	    String codVol = "";
		String usoProd = "";
	    
	    BigDecimal codPedido = BigDecimal.ZERO;
	    
	    BigDecimal codparc;
	    String cnpj;
	    String data;
	    String pedidoString;
	    
	    
	    String statusErro;
	    
	    boolean confirm = false;
		
		JsonParser parser = new JsonParser();
	    JsonElement jsonElement = parser.parse(response);
	    JsonArray pedidos = jsonElement.getAsJsonArray();
		//ctx.setMensagemRetorno("" + pedidos.size());
	    
	    try{
	    	for (int i = 0; i < pedidos.size(); i++) {
	    		
	    		JsonObject pedido = (JsonObject) pedidos.get(i);
		        JsonArray itens = pedido.getAsJsonArray("itens");
		        
		        codPedido = new BigDecimal(pedido.get("pedido").getAsString());
		        codparc = getParc(pedido.get("cnpj").getAsString());
		        cnpj = new String(pedido.get("cnpj").getAsString()); 
		        data = pedido.get("data").getAsString();
		        pedidoString = pedido.toString();
		        codparc = getParc(pedido.get("cnpj").getAsString());//new BigDecimal("9277");
		        tabPreco = new BigDecimal(parametro.get("tabPreco"));
		        codlocal = new BigDecimal(parametro.get("locEst"));
		        
		       
		        inserirTelaTelecon(codPedido, codparc, cnpj, data);

		        if (!verificarEstoque(codlocal, new BigDecimal(codEmp), codprod)) {
		        	statusErro = "Sem estoque";
		        	updateStatus(statusErro, codPedido);
				}
		       
		        
	    	}
	    }catch(Exception e) {
	        e.printStackTrace();
	    }
	    
	    	

	}
	
public boolean verificarEstoque(BigDecimal codlocal, BigDecimal codEmp, BigDecimal codprod) throws Exception{
		
		EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = entityFacade.getJdbcWrapper();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		BigDecimal estoque = null;
		
		jdbc.openSession();
		
		String query = "SELECT ISNULL(TGFEST.ESTOQUE, 0) AS ESTOQUE \r\n"
				+ "FROM (SELECT 1 AS N) AS N \r\n"
				+ "LEFT JOIN sankhya.TGFEST ON TGFEST.CODLOCAL = ?\r\n"
				+ "AND TGFEST.CODEMP = ?\r\n"
				+ "AND TGFEST.CODPROD = ?\r\n"
				+ "AND TGFEST.CODPARC = 17255\r\n"
				+ "\r\n"
				+ "";
		
		pstmt = jdbc.getPreparedStatement(query);
		pstmt.setBigDecimal(1, codlocal);
		pstmt.setBigDecimal(2, codEmp);
		pstmt.setBigDecimal(3, codprod);
		
		rs = pstmt.executeQuery();
		
		while(rs.next()){
			
			estoque = rs.getBigDecimal("ESTOQUE");
			
		}
		
		if (rs != null) {
			rs.close();
		}
		if (pstmt != null) {
			pstmt.close();
		}
		jdbc.closeSession();
		
		if(estoque.compareTo(BigDecimal.ZERO) != 0){
			return true;
		}else{
			return false;
		}
		
	}
	
public String getTipMov(BigDecimal codtipoper) throws Exception{
		
		EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = entityFacade.getJdbcWrapper();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		String tipMov = null;
		
		jdbc.openSession();
		
		String query = "SELECT TIPMOV FROM TGFTOP WHERE CODTIPOPER = ?";
		
		pstmt = jdbc.getPreparedStatement(query);
		pstmt.setBigDecimal(1, codtipoper);
		
		rs = pstmt.executeQuery();
		
		while(rs.next()){
			
			tipMov = rs.getString("TIPMOV");
			
		}
		
		if (rs != null) {
			rs.close();
		}
		if (pstmt != null) {
			pstmt.close();
		}
		jdbc.closeSession();
		
		return tipMov;
	}
	
public String getDhtTipOper(BigDecimal codtipoper) throws Exception{
		
		EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = entityFacade.getJdbcWrapper();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		SimpleDateFormat formatter2 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		
		String dhalter = null;
		
		try{
			
			jdbc.openSession();
			
			String query = "SELECT MAX(DHALTER) AS DHALTER FROM TGFTOP WHERE CODTIPOPER = ?";
			
			pstmt = jdbc.getPreparedStatement(query);
			pstmt.setBigDecimal(1, codtipoper);
			
			rs = pstmt.executeQuery();
			
			while(rs.next()){
				
				dhalter = formatter2.format(rs.getTimestamp("dhalter"));
				
			}
			
		}catch (SQLException e) {
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
		
		return dhalter.toString();
	}
	
public String getDhtTipVend(BigDecimal codtipvend) throws Exception{
		
		EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = entityFacade.getJdbcWrapper();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		SimpleDateFormat formatter2 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		
		String dhalter = null;
		
		jdbc.openSession();
		
		String query = "SELECT MAX(DHALTER) AS DHALTER FROM TGFTPV WHERE CODTIPVENDA = ?";
		
		pstmt = jdbc.getPreparedStatement(query);
		pstmt.setBigDecimal(1, codtipvend);
		
		rs = pstmt.executeQuery();
		
		while(rs.next()){
			
			dhalter = formatter2.format(rs.getTimestamp("dhalter"));
			
		}
		
		if (rs != null) {
			rs.close();
		}
		if (pstmt != null) {
			pstmt.close();
		}
		jdbc.closeSession();
		
		return dhalter.toString();
	}
	
public BigDecimal getParc(String cnpj) throws Exception{
		
		EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = entityFacade.getJdbcWrapper();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		BigDecimal codparc = null;
		
		jdbc.openSession();
		
		String query = "SELECT CODPARC AS CODPARC FROM TGFPAR WHERE CGC_CPF = ?";
		
		pstmt = jdbc.getPreparedStatement(query);
		pstmt.setString(1, cnpj);
		
		rs = pstmt.executeQuery();
		
		while(rs.next()){
			
			codparc = rs.getBigDecimal("CODPARC");
			
		}
		
		if (rs != null) {
			rs.close();
		}
		if (pstmt != null) {
			pstmt.close();
		}
		
		jdbc.closeSession();
		
		return codparc;
	}
	
	
	public Map<String, String> obterParametro() throws Exception {
		EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = entityFacade.getJdbcWrapper();
	    PreparedStatement pstmt = null;
	    ResultSet rs = null; 
	    
	    Map<String, String> parametro = null;
	    
	    try {
	        jdbc.openSession();
	        
	        String sqlP = "SELECT \r\n"
	        		+ "MAX(CASE WHEN PARAMETRO = 'Centro de Resultado para Telecontrol Garantia' THEN VALOR END) AS 'CenResultGar', \r\n"
	        		+ "MAX(CASE WHEN PARAMETRO = 'Centro de Resultado para Telecontrol Venda' THEN VALOR END) AS 'CenResultVen', \r\n"
	        		+ "MAX(CASE WHEN PARAMETRO = 'CIF/FOB para Telecontrol Garantia' THEN VALOR END) AS 'CIF/FOB Gar', \r\n"
	        		+ "MAX(CASE WHEN PARAMETRO = 'CIF/FOB para Telecontrol Venda' THEN VALOR END) AS 'CIF/FOB Ven', \r\n"
	        		+ "MAX(CASE WHEN PARAMETRO = 'Cod Projeto para Telecontrol' THEN VALOR END) AS 'Cod Projeto', \r\n"
	        		+ "MAX(CASE WHEN PARAMETRO = 'Cod Transportadora para Telecontrol' THEN VALOR END) AS 'Cod Transportadora', \r\n"
	        		+ "MAX(CASE WHEN PARAMETRO = 'Empresa do Telecontrol' THEN VALOR END) AS 'Empresa', \r\n"
	        		+ "MAX(CASE WHEN PARAMETRO = 'Local de Estoque OS Telecontrol' THEN VALOR END) AS 'Local de Estoque', \r\n"
	        		+ "MAX(CASE WHEN PARAMETRO = 'Natureza para OS Telecontrol Garantia' THEN VALOR END) AS 'Natureza Garantia', \r\n"
	        		+ "MAX(CASE WHEN PARAMETRO = 'Natureza para OS Telecontrol Venda' THEN VALOR END) AS 'Natureza Venda', \r\n"
	        		+ "MAX(CASE WHEN PARAMETRO = 'Tabela de preco para OS Telecontrol' THEN VALOR END) AS 'Tabela de preco', \r\n"
	        		+ "MAX(CASE WHEN PARAMETRO = 'Tipo de Negociacao para OS Telecontrol Garantia' THEN VALOR END) AS 'Tipo de Negociacao Garantia', \r\n"
	        		+ "MAX(CASE WHEN PARAMETRO = 'Tipo de Negociação para OS Telecontrol Venda' THEN VALOR END) AS 'Tipo de Negociacao Venda', \r\n"
	        		+ "MAX(CASE WHEN PARAMETRO = 'TOP para OS Telecontrol Garantia' THEN VALOR END) AS 'TOP para Garantia', \r\n"
	        		+ "MAX(CASE WHEN PARAMETRO = 'TOP para OS Telecontrol Venda' THEN VALOR END) AS 'TOP para Venda' \r\n"
	        		+ "FROM SANKHYA.AD_PARAMETROS \r\n"
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
	        	parametro.put("tabPreco", rs.getString("Tabela de preco"));
	        	parametro.put("tipNegGar", rs.getString("Tipo de Negociao Garantia"));
	        	parametro.put("tipNegVen", rs.getString("Tipo de Negociacao Venda"));
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
	
	

}
