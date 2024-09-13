package br.com.sankhya.telecontrol;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.google.gson.JsonObject;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class CadastraProduto implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao ctx) throws Exception {
		// TODO Auto-generated method stub
		try{
			apiPostPecas(ctx, "http://api2.telecontrol.com.br/posvenda-core/peca");
			
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
	
public static void apiPostPecas(ContextoAcao ctx, String ur) throws Exception {
		
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
	    String sql = "SELECT PRO.CODPROD, PRO.QTDEMB, RTRIM(PRO.DESCRPROD) AS DESCRPROD, PRO.REFERENCIA, PRO.ORIGPROD, PRO.ATIVO,\r\n"
	    		+ "PRO.AD_BLOQUEADOVENDATELECONTROL, PRO.AD_BLOQUEADOGARANTIATELECONTROL, \r\n"
	    		+ "PRO.AD_ACESSORIOTELECONTROL, PRO.CODVOL, format(IPI.PERCENTUAL, '0.00') as IPI, \r\n"
	    		+ "format(EST.ESTOQUE, '0.00') as ESTOQUE \r\n"
	    		+ "FROM SANKHYA.TGFPRO AS PRO \r\n"
	    		+ "INNER JOIN SANKHYA.TGFIPI IPI ON IPI.CODIPI = PRO.CODIPI \r\n"
	    		+ "INNER JOIN SANKHYA.TGFEST EST ON EST.CODPROD = PRO.CODPROD \r\n"
	    		+ "WHERE \r\n"
	    		+ "EST.CODLOCAL = 1030000  \r\n"
	    		+ "AND PRO.CODPROD = 2006002";
	    
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
	        	ctx.setMensagemRetorno("teste");
	        	update(codProd, "TGFPRO", "AD_ENVIADOTELECONTROL", "CODPROD");
	        }
	        
	        in.close();
	
	        // Exibindo a resposta
	        System.out.println(response.toString());
	    }
	        jdbc.closeSession();		
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


}
