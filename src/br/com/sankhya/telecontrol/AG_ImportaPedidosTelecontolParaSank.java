package br.com.sankhya.telecontrol;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Timestamp;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.cuckoo.core.ScheduledAction;
import org.cuckoo.core.ScheduledActionContext;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sankhya.util.JsonUtils;

import br.com.sankhya.telecontrol.services.SkwServicoCompras;
import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.core.JapeSession.SessionHandle;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class AG_ImportaPedidosTelecontolParaSank implements ScheduledAction {

	@Override
	public void onTime(ScheduledActionContext arg0) {

		try {
			apiFaturamento();
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
		http.addRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
		http.setRequestProperty("Access-Application-Key", key);
		http.setRequestProperty("Access-Env", "PRODUCTION");
		http.setRequestProperty("Content-Type", "application/json");
		http.setDoOutput(true);
		http.setDoInput(true);

		int status = http.getResponseCode();

		if (status >= 300) {
			reader = new BufferedReader(new InputStreamReader(http.getErrorStream()));
			while ((line = reader.readLine()) != null) {
				responseContent.append(line);
			}
			reader.close();
		} else {
			reader = new BufferedReader(new InputStreamReader(http.getInputStream()));
			while ((line = reader.readLine()) != null) {
				responseContent.append(line);
			}
			reader.close();
		}
		System.out.println("Output from Server .... \n" + status);
		String response = responseContent.toString();

		http.disconnect();

		return new String[] { Integer.toString(status), response };

	}

	public void apiFaturamento() throws Exception {
		LocalDate currentDate = LocalDate.now();

		// Subtrair 5 dias da data atual
		LocalDate fiveDaysAgo = currentDate.minusDays(1);

		// Formatando as datas
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		String formattedCurrentDate = currentDate.format(formatter);
		String formattedFiveDaysAgo = fiveDaysAgo.format(formatter);

		System.out.println("Data da API: " + formattedCurrentDate);
		System.out.println("Data da API A 5 DIAS: " + formattedFiveDaysAgo);

		String[] result = api("http://api2.telecontrol.com.br/posvenda-pedido/pedidos/dataInicial/"
				+ formattedFiveDaysAgo + "/dataFinal/" + formattedCurrentDate);

		SkwServicoCompras sc = null;

		try {

			SessionHandle hnd = JapeSession.open();

			System.out.println("Entrou aqui JOBFaturamento");

			// JapeWrapper parametrosExtDAO = JapeFactory
			// .dao("ParametrosExtensoes");
			// DynamicVO parametroVO;

			// parametroVO = parametrosExtDAO
			// .findByPK("DOMAIN_SERVICE");

			// parametroVO.asString("VALOR");
			String domain = "http://192.168.0.101:8180";

			JapeWrapper usuarioDAO = JapeFactory.dao(DynamicEntityNames.USUARIO);
			DynamicVO usuarioVO = usuarioDAO.findByPK(new BigDecimal(0));
			String md5 = usuarioVO.getProperty("INTERNO").toString();
			String nomeUsu = usuarioVO.getProperty("NOMEUSU").toString();

			sc = new SkwServicoCompras(domain, nomeUsu, md5);

			// JapeSession.close(hnd);
		} catch (Exception e2) {
			e2.printStackTrace();
		}

		System.out.println("Instanciou A API");

		String response = result[1];
		BigDecimal nuNota = null;

		Map<String, String> parametro;

		parametro = obterParametro();

		String codEmp = parametro.get("codEmp");
		String top = "";
		String adEntrega = "";
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

		boolean confirm = false;

		JsonParser parser = new JsonParser();
		JsonElement jsonElement = parser.parse(response);
		JsonArray pedidos = jsonElement.getAsJsonArray();

		System.out.println("Declarou variaveis");

		try {

			System.out.println("Size: " + pedidos.size());

			for (int i = 0; i < pedidos.size(); i++) {

				System.out.println("Entrou no primeiro for");
				JsonObject pedido = (JsonObject) pedidos.get(i);
				JsonArray itens = pedido.getAsJsonArray("itens");
				codPedido = new BigDecimal(pedido.get("pedido").getAsString());
				codparc = getParc(pedido.get("cnpj").getAsString());// new BigDecimal("9277");
				tabPreco = new BigDecimal(parametro.get("tabPreco"));
				codlocal = new BigDecimal(parametro.get("locEst"));

				if (pedido.get("codigo").getAsString().equals("GAR")) {
					codTipNeg = parametro.get("tipNegGar");
				} else {
					codTipNeg = parametro.get("tipNegVen");
				}

				if (pedido.get("codigo").getAsString().equals("GAR")) {
					top = parametro.get("topGar");
					adEntrega = parametro.get("adEntr");
				} else {
					top = parametro.get("topVen");
				}

				if (pedido.get("codigo").getAsString().equals("GAR")) {
					codcecus = parametro.get("cenReGar");
				} else {
					codcecus = parametro.get("cenReVen");
				}

				if (pedido.get("codigo").getAsString().equals("GAR")) {
					natureza = parametro.get("natGar");
				} else {
					natureza = parametro.get("natVen");
				}

				if (pedido.get("codigo").getAsString().equals("GAR")) {
					cifFob = parametro.get("cifFobGar");
				} else {
					cifFob = parametro.get("cifFobVen");
				}

				dhTipNeg = getDhtTipVend(new BigDecimal(codTipNeg));
				dhTop = getDhtTipOper(new BigDecimal(top));
				tipMov = getTipMov(new BigDecimal(top));

				for (int j = 0; j < itens.size(); j++) {
					JsonObject item = (JsonObject) itens.get(j);

					System.out.println("Teste Referencia: " + item.get("referencia").getAsString());

					referencia = getCodProd(item.get("referencia").getAsString());

					codprodVar = new BigDecimal(getCodProd(item.get("referencia").getAsString()));

					System.out.println("codlocal: " + codlocal + " codEmp: " + codEmp + " codprod: " + codprodVar);
					if (verificarEstoque(codlocal, new BigDecimal(codEmp), codprodVar)) {
						quantidade = item.get("qtde").getAsString();

						qtdVol = qtdVol.add(new BigDecimal(quantidade));

						pesoLiq = pesoLiq.add(getPesoLiq(new BigDecimal(referencia)));
						pesoBruto = pesoBruto.add(getPesoBruto(new BigDecimal(referencia)));

					}
				}

				topBg = new BigDecimal(top);
				codTipNegBg = new BigDecimal(codTipNeg);
				codcecusBg = new BigDecimal(codcecus);
				naturezaBg = new BigDecimal(natureza);
				codEmpBg = new BigDecimal(codEmp);

				System.out.println("Chegou na nota");
				System.out.println("empresa: " + codEmp + " top: " + top + " dhtop: " + dhTop + " tipmov: " + tipMov
						+ " codparc: " + codparc + " codTipNeg: " + codTipNeg + " dhtipneg: " + dhTipNeg + " codcecus: "
						+ codcecus + " natureza: " + natureza + " qtdVol: " + qtdVol + " pesliq: " + pesoLiq
						+ " pesobruto: " + pesoBruto + " Entrega: " + adEntrega);

//		        ctx.setMensagemRetorno("empresa: " + codEmp + " top: " + top + " dhtop: " + dhTop
//		        		+ " tipmov: " + tipMov + " codparc: " + codparc + " codTipNeg: " + codTipNeg
//		        		+ " dhtipneg: " + dhTipNeg + " codcecus: " + codcecus + " natureza: " + natureza
//		        		+ " qtdVol: " + qtdVol + " pesliq: " + pesoLiq + " pesobruto: " + pesoBruto);
//		        
				try {
					// updTgfNum();

					// nuNota = getMaxNumCab();

					// System.out.println(nuNota);

					// if(nuNota != null){

					nunota = sc.saveCabecalhoNotaVonixx(null, codEmp, topBg.toString(), 
		        			dhTop, tipMov, codparc.toString(), codTipNegBg.toString(), 
		        			dhTipNeg, codcecusBg.toString(), naturezaBg.toString(), 
		        			qtdVol.longValue(), pesoLiq, pesoBruto, cifFob, codPedido.toString(), null, adEntrega.toString());
			        	
			        	System.out.println(nunota);

					System.out.println(nunota);

					/*
					 * confirm = insertCab(nuNota, topBg, tipMov, dhTop, codTipNegBg, dhTipNeg,
					 * codcecusBg, codEmpBg, codparc, naturezaBg, qtdVol, pesoLiq, pesoBruto);
					 */

					// }

					System.out.println("Criou a nota");

					for (int j = 0; j < itens.size(); j++) {
						System.out.println("chegou no for dos itens");

						JsonObject item = (JsonObject) itens.get(j);

						codprod = new BigDecimal(getCodProd(item.get("referencia").getAsString()));
						pedidoItem = item.get("pedido_item").getAsString();

						produto = obterProduto(codprod);
						codVol = produto.get("codVol");
						usoProd = produto.get("usoProd");
						ipi = new BigDecimal(produto.get("ipi"));

						qtdNeg = new BigDecimal(item.get("qtde").getAsString());
						vlrUnit = getVlrUnit(codprod, tabPreco);
						vlrTotal = vlrUnit.multiply(qtdNeg);

						if (verificarEstoque(codlocal, new BigDecimal(codEmp), codprod)) {
							// if(confirm){
							System.out.println("nunota: " + nunota + " codEmp: " + codEmp + " codprod: " + codprod
									+ " quantidade: " + qtdNeg + " vlrUnit: " + vlrUnit + " vlrTotal: " + vlrTotal
									+ " usoProd: " + usoProd + " codVol: " + codVol + " codlocal: " + codlocal);

							insertIte(new BigDecimal(nunota), new BigDecimal(codEmp), codprod, qtdNeg, vlrUnit,
									vlrTotal, usoProd, codVol, codlocal, pedidoItem, ipi);

							System.out.println("Inseriu");
							// }
						} else {
							System.out.println("Sem estoque para o produto: " + codprod + " no local: " + codlocal);
						}
					}

					if (nunota != null) {

						updVlrNota(getVlrNota(new BigDecimal(nunota)), new BigDecimal(nunota));
						putExportPedido("http://api2.telecontrol.com.br/posvenda-pedido/pedidos/pedido/" + codPedido);
						insertInfoPedido(new BigDecimal(nunota));

					}

				} catch (Exception e) {
					if (nunota != null) {
						deleteNota(new BigDecimal(nunota));
						deleteItem(new BigDecimal(nunota));
					}
					e.printStackTrace();
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void inserirPar(ScheduledActionContext contexto) throws Exception {

		EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = entityFacade.getJdbcWrapper();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		StringBuffer mensagem = new StringBuffer();

		jdbc.openSession();
		String[] response = api(
				"https://private-anon-fe98d2f5d6-posvendamestre.apiary-mock.com/posvenda-core/revendaFabrica");

		if (response[0].equals("200")) {
			JsonElement jelement = new JsonParser().parse(response[1]);
			JsonArray teste = jelement.getAsJsonArray();
			JsonObject obj = (JsonObject) teste.get(0);

			String cnpj = null;
			String razao = null;
			String tippessoa = "J";
			String nomeparc = null;
			int codcid = 1831;
			String alttab = "N";
			String apipar = "S";

			cnpj = JsonUtils.getString(obj, "cnpj");
			razao = JsonUtils.getString(obj, "contato_razao_social");
			nomeparc = JsonUtils.getString(obj, "contato_razao_social");

			// String sql = "SELECT MAX(CODPARC)+1 FROM TGFPAR"; pstmt =
			// jdbc.getPreparedStatement(sql); rs = pstmt.executeQuery(); while
			// (rs.next()) { codparc = rs.getInt("CODPARC"); }

			if (cnpj != null) {
				String sql_insert = "INSERT INTO TGFPAR (CODPARC, TIPPESSOA, NOMEPARC, "
						+ "CGC_CPF, CODCID, DTCAD, DTALTER, RAZAOSOCIAL, AD_ALTTABJOB, AD_APIPAR) "
						+ "VALUES ((select max(codparc)+1 from tgfpar), ?, ?, ?, ?, "
						+ "GETDATE(), GETDATE(), ?, ?, ?)";

				pstmt = jdbc.getPreparedStatement(sql_insert);
				pstmt.setString(1, tippessoa);
				pstmt.setString(2, nomeparc);
				pstmt.setString(3, cnpj);
				pstmt.setInt(4, codcid);
				pstmt.setString(5, razao);
				pstmt.setString(6, alttab);
				pstmt.setString(7, apipar);

				pstmt.executeUpdate();
			}
		} else {
			System.out.println("Status: " + response[0]);
			System.out.println("Response: " + response[1]);
		}

		jdbc.closeSession();

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
					+ "MAX(CASE WHEN PARAMETRO = 'Centro de Resultado para Telecontrol Garantia' THEN VALOR END) AS 'CenResultGar',\r\n"
					+ "MAX(CASE WHEN PARAMETRO = 'Centro de Resultado para Telecontrol Venda' THEN VALOR END) AS 'CenResultVen',\r\n"
					+ "MAX(CASE WHEN PARAMETRO = 'CIF/FOB para Telecontrol Garantia' THEN VALOR END) AS 'CIF/FOB Gar',\r\n"
					+ "MAX(CASE WHEN PARAMETRO = 'CIF/FOB para Telecontrol Venda' THEN VALOR END) AS 'CIF/FOB Ven',\r\n"
					+ "MAX(CASE WHEN PARAMETRO = 'Cod Projeto para Telecontrol' THEN VALOR END) AS 'Cod Projeto',\r\n"
					+ "MAX(CASE WHEN PARAMETRO = 'Cod Transportadora para Telecontrol' THEN VALOR END) AS 'Cod Transportadora',\r\n"
					+ "MAX(CASE WHEN PARAMETRO = 'Empresa do Telecontrol' THEN VALOR END) AS 'Empresa',\r\n"
					+ "MAX(CASE WHEN PARAMETRO = 'Local de Estoque OS Telecontrol' THEN VALOR END) AS 'Local de Estoque',\r\n"
					+ "MAX(CASE WHEN PARAMETRO = 'Natureza para OS Telecontrol Garantia' THEN VALOR END) AS 'Natureza Garantia',\r\n"
					+ "MAX(CASE WHEN PARAMETRO = 'Natureza para OS Telecontrol Venda' THEN VALOR END) AS 'Natureza Venda',\r\n"
					+ "MAX(CASE WHEN PARAMETRO = 'Tabela de preco para OS Telecontrol' THEN VALOR END) AS 'Tabela de preco',\r\n"
					+ "MAX(CASE WHEN PARAMETRO = 'Tipo de Negociacao para OS Telecontrol Garantia' THEN VALOR END) AS 'Tipo de Negociacao Garantia',\r\n"
					+ "MAX(CASE WHEN PARAMETRO = 'Tipo de Negociacao para OS Telecontrol Venda' THEN VALOR END) AS 'Tipo de Negociacao Venda',\r\n"
					+ "MAX(CASE WHEN PARAMETRO = 'TOP para OS Telecontrol Garantia' THEN VALOR END) AS 'TOP para Garantia',\r\n"
					+ "MAX(CASE WHEN PARAMETRO = 'TOP para OS Telecontrol Venda' THEN VALOR END) AS 'TOP para Venda',\r\n"
					+ "MAX(CASE WHEN PARAMETRO = 'Entrega para OS Telecontrol Garantia' THEN VALOR END) AS 'AD_ENTR para Garantia'\r\n"
					+ "FROM SANKHYA.AD_PARAMETROS\r\n" + "WHERE PARAMETRO LIKE '%Telecontrol%'";

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
				parametro.put("tipNegGar", rs.getString("Tipo de Negociacao Garantia"));
				parametro.put("tipNegVen", rs.getString("Tipo de Negociacao Venda"));
				parametro.put("topGar", rs.getString("TOP para Garantia"));
				parametro.put("topVen", rs.getString("TOP para Venda"));
				parametro.put("adEntr", rs.getString("AD_ENTR para Garantia"));

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

	public String getDhtTipOper(BigDecimal codtipoper) throws Exception {

		EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = entityFacade.getJdbcWrapper();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		SimpleDateFormat formatter2 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

		String dhalter = null;

		try {

			jdbc.openSession();

			String query = "SELECT MAX(DHALTER) AS DHALTER FROM TGFTOP WHERE CODTIPOPER = ?";

			pstmt = jdbc.getPreparedStatement(query);
			pstmt.setBigDecimal(1, codtipoper);

			rs = pstmt.executeQuery();

			while (rs.next()) {

				dhalter = formatter2.format(rs.getTimestamp("dhalter"));

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

		return dhalter.toString();
	}

	public void updTgfNum() throws Exception {

		EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = entityFacade.getJdbcWrapper();
		PreparedStatement pstmt = null;

		try {
			jdbc.openSession();

			String sqlUpd = "UPDATE TGFNUM SET ULTCOD = ULTCOD + 1  WHERE ARQUIVO = 'TGFCAB'";

			pstmt = jdbc.getPreparedStatement(sqlUpd);
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

	public void updVlrNota(BigDecimal vlrnota, BigDecimal nunota) throws Exception {

		EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = entityFacade.getJdbcWrapper();
		PreparedStatement pstmt = null;

		try {
			jdbc.openSession();

			String sqlUpd = "UPDATE TGFCAB SET VLRNOTA = ?  WHERE NUNOTA = ?";

			pstmt = jdbc.getPreparedStatement(sqlUpd);
			pstmt.setBigDecimal(1, vlrnota);
			pstmt.setBigDecimal(2, nunota);

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

	public String getTipMov(BigDecimal codtipoper) throws Exception {

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

		while (rs.next()) {

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

	public boolean verificarEstoque(BigDecimal codlocal, BigDecimal codemp, BigDecimal codprod) throws Exception {

		EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = entityFacade.getJdbcWrapper();
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		BigDecimal estoque = null;

		jdbc.openSession();

		String query = "SELECT\r\n"
				+ "COALESCE(EST2.ESTOQUE + COALESCE(VQ.QTDNEG,0) - COALESCE(NOTA.QTDNEG,0) - EST3.RESERVADO - COALESCE(PED.QTDNEG,0),0) AS 'ESTOQUE'\r\n"
				+ "FROM SANKHYA.TGFPRO MARACA\r\n"
				+ "LEFT JOIN SANKHYA.TGFEST EST1 ON MARACA.CODPROD = EST1.CODPROD AND EST1.CODEMP = 9 AND EST1.CONTROLE = '' AND EST1.CODLOCAL = 2080000 AND EST1.ATIVO ='S'\r\n"
				+ "LEFT JOIN SANKHYA.TGFEST EST2 ON MARACA.CODPROD = EST2.CODPROD AND EST2.CODLOCAL = 1200000 AND EST2.CODPARC = 265 AND EST2.CODEMP = 16 AND EST2.CONTROLE = ''\r\n"
				+ "LEFT JOIN SANKHYA.TGFEST EST3 ON MARACA.CODPROD = EST3.CODPROD AND EST3.CODLOCAL = 1030000 AND EST3.CODPARC = 0 AND EST3.CODEMP = 1 AND EST3.CONTROLE = ''\r\n"
				+ "\r\n" + "LEFT JOIN (SELECT CODPROD, SUM(QTDNEG) AS QTDNEG\r\n" + "FROM SANKHYA.TGFITE VQ \r\n"
				+ "WHERE VQ.NUNOTA IN (24,25) \r\n" + "GROUP BY CODPROD) VQ ON MARACA.CODPROD = VQ.CODPROD\r\n" + "\r\n"
				+ "LEFT JOIN (SELECT CODPROD, SUM(QTDNEG) AS QTDNEG\r\n" + "FROM SANKHYA.TGFITE PED \r\n"
				+ "WHERE PED.NUNOTA IN (SELECT TGFCAB.NUNOTA FROM SANKHYA.TGFCAB\r\n"
				+ "WHERE TGFCAB.CODTIPOPER IN (48,52,54,57,70,154,152,3200,3201,167,2014,3206,9123,1121,9156,9160,9164,9158,9187,9165) \r\n"
				+ "AND TGFCAB.CODEMP = 1 \r\n" + "AND TGFCAB.STATUSNFE <> 'A')\r\n"
				+ "GROUP BY CODPROD) PED ON MARACA.CODPROD = PED.CODPROD\r\n" + "\r\n"
				+ "LEFT JOIN (SELECT CODPROD, SUM(QTDNEG) AS QTDNEG\r\n" + "FROM SANKHYA.TGFITE NOTA \r\n"
				+ "WHERE NOTA.NUNOTA IN ((SELECT NUNOTA FROM SANKHYA.TGFCAB CAB1\r\n" + "WHERE CAB1.CODEMP = 1 \r\n"
				+ "AND CAB1.CODPARC = 17255 \r\n" + "AND CAB1.CODTIPOPER = 9150 \r\n" + "AND CAB1.NUMNOTA <> 0 \r\n"
				+ "AND (SELECT TOP 1 NUNOTA FROM SANKHYA.TGFCAB CAB2 WHERE CAB2.NUMNOTA = CAB1.NUMNOTA AND CAB2.CODEMP = 16 AND CAB2.CODPARC = 265) IS NULL)) \r\n"
				+ "GROUP BY CODPROD) NOTA ON MARACA.CODPROD = NOTA.CODPROD\r\n" + "\r\n" + "WHERE MARACA.CODPROD = ?";

		pstmt = jdbc.getPreparedStatement(query);

		pstmt.setBigDecimal(1, codprod);

//		String query = "SELECT ISNULL(TGFEST.ESTOQUE, 0) AS ESTOQUE \r\n"
//				+ "FROM (SELECT 1 AS N) AS N \r\n"
//				+ "LEFT JOIN sankhya.TGFEST ON TGFEST.CODLOCAL = ?\r\n"
//				+ "AND TGFEST.CODEMP = ?\r\n"
//				+ "AND TGFEST.CODPROD = ?\r\n"
//				+ "AND TGFEST.CODPARC = 17255\r\n"
//				+ "\r\n"
//				+ "";
//		
//		pstmt = jdbc.getPreparedStatement(query);
//		pstmt.setBigDecimal(1, codlocal);
//		pstmt.setBigDecimal(2, codemp);
//		pstmt.setBigDecimal(3, codprod);

		rs = pstmt.executeQuery();

		while (rs.next()) {

			estoque = rs.getBigDecimal("ESTOQUE");

		}

		if (rs != null) {
			rs.close();
		}
		if (pstmt != null) {
			pstmt.close();
		}
		jdbc.closeSession();

		if (estoque.compareTo(BigDecimal.ZERO) != 0) {
			return true;
		} else {
			return false;
		}

	}

	public BigDecimal getParc(String cnpj) throws Exception {

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

		while (rs.next()) {

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

	public String getDhtTipVend(BigDecimal codtipvend) throws Exception {

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

		while (rs.next()) {

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

	public BigDecimal getMaxNumCab() throws Exception {

		EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = entityFacade.getJdbcWrapper();
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		BigDecimal nunota = null;

		jdbc.openSession();

		String query = "SELECT MAX(ULTCOD) AS ID FROM TGFNUM WHERE ARQUIVO = 'TGFCAB'";

		pstmt = jdbc.getPreparedStatement(query);

		rs = pstmt.executeQuery();

		while (rs.next()) {

			nunota = rs.getBigDecimal("ID");

		}

		if (rs != null) {
			rs.close();
		}
		if (pstmt != null) {
			pstmt.close();
		}
		jdbc.closeSession();

		return nunota;
	}

	public BigDecimal getPesoLiq(BigDecimal codprod) throws Exception {

		EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = entityFacade.getJdbcWrapper();
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		BigDecimal pesLiq = null;

		jdbc.openSession();

		String query = "SELECT PESOLIQ FROM TGFPRO WHERE CODPROD = ?";

		pstmt = jdbc.getPreparedStatement(query);
		pstmt.setBigDecimal(1, codprod);

		rs = pstmt.executeQuery();

		while (rs.next()) {

			pesLiq = rs.getBigDecimal("PESOLIQ");

		}

		if (rs != null) {
			rs.close();
		}
		if (pstmt != null) {
			pstmt.close();
		}
		jdbc.closeSession();

		return pesLiq;
	}

	public BigDecimal getPesoBruto(BigDecimal codprod) throws Exception {

		EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = entityFacade.getJdbcWrapper();
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		BigDecimal pesoBruto = null;

		jdbc.openSession();

		String query = "SELECT PESOBRUTO FROM TGFPRO WHERE CODPROD = ?";

		pstmt = jdbc.getPreparedStatement(query);
		pstmt.setBigDecimal(1, codprod);

		rs = pstmt.executeQuery();

		while (rs.next()) {

			pesoBruto = rs.getBigDecimal("PESOBRUTO");

		}

		if (rs != null) {
			rs.close();
		}
		if (pstmt != null) {
			pstmt.close();
		}
		jdbc.closeSession();

		return pesoBruto;
	}

	public boolean insertCab(BigDecimal nunota, BigDecimal tipOper, String tipMov, String dhTipOper,
			BigDecimal tipVenda, String dhTipVenda, BigDecimal codCenCus, BigDecimal codEmp, BigDecimal codparc,
			BigDecimal natureza, BigDecimal qtdVol, BigDecimal pesoLiq, BigDecimal pesoBruto) throws Exception {

		EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = entityFacade.getJdbcWrapper();
		PreparedStatement pstmt = null;

		try {

			jdbc.openSession();

			String sqlUpdate = "INSERT INTO TGFCAB(NUNOTA, " + "NUMNOTA, " + "DTNEG, " + "CODTIPOPER, " + "TIPMOV, "
					+ "DHTIPOPER, " + "CODTIPVENDA, " + "DHTIPVENDA, " + "CODPARC, " + "CODEMP, " + "CODEMPNEGOC, "
					+ "CODCENCUS, " + "CODNAT, " + "QTDVOL, " + "PESO, " + "PESOBRUTO, " + "DTALTER, " + "DTMOV) "
					+ "  VALUES (?, " + "           0, " + "           CURRENT_TIMESTAMP, " + "           ?, "
					+ "           ?, " + "           ?, " + "           ?, " + "           ?, " + "           ?, "
					+ "           ?, " + "           ?, " + "           ?, " + "           ?, " + "           ?, "
					+ "          ?, " + "          ?, " + "          CURRENT_TIMESTAMP, "
					+ "           CURRENT_TIMESTAMP);";

			pstmt = jdbc.getPreparedStatement(sqlUpdate);
			pstmt.setBigDecimal(1, nunota);
			pstmt.setBigDecimal(2, tipOper);
			pstmt.setString(3, tipMov);
			pstmt.setString(4, dhTipOper);
			pstmt.setBigDecimal(5, tipVenda);
			pstmt.setString(6, dhTipVenda);
			pstmt.setBigDecimal(7, codparc);
			pstmt.setBigDecimal(8, codEmp);
			pstmt.setBigDecimal(9, codEmp);
			pstmt.setBigDecimal(10, codCenCus);
			pstmt.setBigDecimal(11, natureza);
			pstmt.setBigDecimal(12, qtdVol);
			pstmt.setBigDecimal(13, pesoLiq);
			pstmt.setBigDecimal(14, pesoBruto);
			pstmt.executeUpdate();

		} catch (Exception se) {
			se.printStackTrace();
			return false;
		} finally {
			if (pstmt != null) {
				pstmt.close();
			}

			if (jdbc != null) {
				jdbc.closeSession();
			}
		}

		return true;

	}

	public void insertIte(BigDecimal nunota, BigDecimal codemp, BigDecimal codprod, BigDecimal qtdNeg,
			BigDecimal vlrUnit, BigDecimal vlrTotal, String usoprod, String codvol, BigDecimal codlocal,
			String pedidoItem, BigDecimal ipi) throws Exception {
		EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = entityFacade.getJdbcWrapper();
		PreparedStatement pstmt = null;

		jdbc.openSession();

		String sqlUpdate = "INSERT INTO TGFITE(NUNOTA, " + "SEQUENCIA, " + "CODEMP, " + "CODPROD, " + "QTDNEG, "
				+ "VLRUNIT, " + "VLRTOT," + "USOPROD," + "CODVOL," + "CODLOCALORIG," + "ATUALESTOQUE," + "RESERVA,"
				+ "AD_PEDIDO_ITEM_TELECONTROL," + "BASEIPI," + "ALIQIPI," + "VLRIPI) " + "  VALUES (?, "
				+ "           (SELECT ISNULL(MAX(SEQUENCIA), 0) + 1 FROM TGFITE WHERE NUNOTA = ?), " + "           ?, "
				+ "           ?, " + "           ?, " + "           ?, " + "           ?," + "			 ?,"
				+ "			 ?," + "			 ?," + "			 1," + "			 'S'," + "			  ?,"
				+ "			  ?," + "			  ?," + "			  ?) ";

		pstmt = jdbc.getPreparedStatement(sqlUpdate);
		pstmt.setBigDecimal(1, nunota);
		pstmt.setBigDecimal(2, nunota);
		pstmt.setBigDecimal(3, codemp);
		pstmt.setBigDecimal(4, codprod);
		pstmt.setBigDecimal(5, qtdNeg);
		pstmt.setBigDecimal(6, vlrUnit);
		pstmt.setBigDecimal(7, vlrTotal);
		pstmt.setString(8, usoprod);
		pstmt.setString(9, codvol);
		pstmt.setBigDecimal(10, codlocal);
		pstmt.setString(11, pedidoItem);
		pstmt.setBigDecimal(12, vlrTotal);
		pstmt.setBigDecimal(13, ipi);
		pstmt.setBigDecimal(14, vlrTotal.multiply(ipi).divide(BigDecimal.valueOf(100)));
		pstmt.executeUpdate();

		try {
			if (pstmt != null) {
				pstmt.close();
			}
			if (jdbc != null) {
				jdbc.closeSession();
			}
		} catch (Exception se) {
			se.printStackTrace();
		}

	}

	public void deleteNota(BigDecimal nunota) throws Exception {

		EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = entityFacade.getJdbcWrapper();
		PreparedStatement pstmt = null;

		jdbc.openSession();

		String sqlUpdate = "DELETE FROM TGFCAB WHERE NUNOTA = ?";

		pstmt = jdbc.getPreparedStatement(sqlUpdate);
		pstmt.setBigDecimal(1, nunota);
		pstmt.executeUpdate();

		try {
			if (pstmt != null) {
				pstmt.close();
			}
			if (jdbc != null) {
				jdbc.closeSession();
			}
		} catch (Exception se) {
			se.printStackTrace();
		}

	}

	public void deleteItem(BigDecimal nunota) throws Exception {

		EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = entityFacade.getJdbcWrapper();
		PreparedStatement pstmt = null;

		jdbc.openSession();

		String sqlUpdate = "DELETE FROM TGFITE WHERE NUNOTA = ?";

		pstmt = jdbc.getPreparedStatement(sqlUpdate);
		pstmt.setBigDecimal(1, nunota);
		pstmt.executeUpdate();

		try {
			if (pstmt != null) {
				pstmt.close();
			}
			if (jdbc != null) {
				jdbc.closeSession();
			}
		} catch (Exception se) {
			se.printStackTrace();
		}

	}

	public BigDecimal getVlrUnit(BigDecimal codprod, BigDecimal codtab) throws Exception {

		EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = entityFacade.getJdbcWrapper();
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		BigDecimal vlrUnit = null;

		jdbc.openSession();

		String query = "SELECT CONVERT(NVARCHAR(50), FORMAT(COALESCE(EXC.VLRVENDA, 0), '0.00')) AS VLRVENDA "
				+ "	FROM TGFTAB TAB " + "			INNER JOIN TGFEXC EXC ON EXC.NUTAB = TAB.NUTAB   "
				+ "			INNER JOIN TGFNTA NTA ON NTA.CODTAB = TAB.CODTAB "
				+ "	WHERE CODPROD = ? AND TAB.CODTAB = ? " + "	UNION ALL "
				+ "	SELECT CONVERT(NVARCHAR(50), FORMAT(0, '0.00')) AS VLRVENDA " + "	WHERE NOT EXISTS ( "
				+ "				SELECT 1 " + "				FROM TGFTAB TAB "
				+ "				INNER JOIN TGFEXC EXC ON EXC.NUTAB = TAB.NUTAB   "
				+ "				INNER JOIN TGFNTA NTA ON NTA.CODTAB = TAB.CODTAB "
				+ "				WHERE CODPROD = ? AND TAB.CODTAB = ? " + "			)";

		pstmt = jdbc.getPreparedStatement(query);
		pstmt.setBigDecimal(1, codprod);
		pstmt.setBigDecimal(2, codtab);
		pstmt.setBigDecimal(3, codprod);
		pstmt.setBigDecimal(4, codtab);

		rs = pstmt.executeQuery();

		while (rs.next()) {

			vlrUnit = new BigDecimal(rs.getString("VLRVENDA").replace(",", "."));

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

	public Map<String, String> obterProduto(BigDecimal codProd) throws Exception {
		EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = entityFacade.getJdbcWrapper();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		Map<String, String> produto = null;

		int codipi = 0;

		try {
			jdbc.openSession();

			String sqlNota = "SELECT codvol, usoprod, codipi FROM tgfpro WHERE codprod = ?";

			pstmt = jdbc.getPreparedStatement(sqlNota);
			pstmt.setBigDecimal(1, codProd);

			rs = pstmt.executeQuery();

			if (rs.next()) {
				produto = new HashMap<>();
				produto.put("codVol", rs.getString("codvol"));
				produto.put("usoProd", rs.getString("usoprod"));

				codipi = rs.getInt("codipi");

				produto.put("ipi", getIpi(codipi).toString());
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

		return produto;
	}

	public BigDecimal getIpi(int codipi) throws Exception {
		EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = entityFacade.getJdbcWrapper();
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		BigDecimal ipi = BigDecimal.ZERO;

		try {
			jdbc.openSession();

			String sqlNota = "SELECT FORMAT(PERCENTUAL, '0.00') IPI FROM TGFIPI WHERE CODIPI = ?";

			pstmt = jdbc.getPreparedStatement(sqlNota);
			pstmt.setInt(1, codipi);

			rs = pstmt.executeQuery();

			if (rs.next()) {

				ipi = new BigDecimal(rs.getString("IPI").replace(",", "."));
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

		return ipi;
	}

	public BigDecimal getVlrNota(BigDecimal nunota) throws Exception {
		EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = entityFacade.getJdbcWrapper();
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		BigDecimal valor = BigDecimal.ZERO;

		try {
			jdbc.openSession();

			String sqlNota = "SELECT FORMAT((SUBQUERY.VLRTOTAL + SUBQUERY.VLRIPI), '0.00') AS TOTAL "
					+ "	FROM (SELECT SUM(VLRTOT) AS VLRTOTAL, SUM(VLRIPI) AS VLRIPI " + "			FROM TGFITE "
					+ "			WHERE NUNOTA = ?) AS SUBQUERY";

			pstmt = jdbc.getPreparedStatement(sqlNota);
			pstmt.setBigDecimal(1, nunota);

			rs = pstmt.executeQuery();

			if (rs.next()) {

				valor = new BigDecimal(rs.getString("TOTAL").replace(",", "."));
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

		return valor;
	}

	public void insertInfoPedido(BigDecimal nunota) throws Exception {

		EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = entityFacade.getJdbcWrapper();
		PreparedStatement pstmt = null;

		jdbc.openSession();

		String sqlUpdate = "INSERT INTO AD_INFOCABTELECONTROL " + " (ID, NUNOTA, FATURADO) "
				+ "  VALUES ((SELECT ISNULL(MAX(ID), 0) + 1 FROM AD_INFOCABTELECONTROL), " + "           ?, "
				+ "			 'N') ";

		pstmt = jdbc.getPreparedStatement(sqlUpdate);
		pstmt.setBigDecimal(1, nunota);
		pstmt.executeUpdate();

		try {
			if (pstmt != null) {
				pstmt.close();
			}
			if (jdbc != null) {
				jdbc.closeSession();
			}
		} catch (Exception se) {
			se.printStackTrace();
		}

	}

	public void putExportPedido(String ur) throws Exception {

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
		System.out.println("Pedido exportado");

	}

	public String preferenciaSenha() throws Exception {

		EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = entityFacade.getJdbcWrapper();
		PreparedStatement pstmt = null;
		String senha = null;
		ResultSet rs = null;

		jdbc.openSession();

		String sqlSenha = "SELECT VALOR FROM SANKHYA.AD_PARAMETROS WHERE " + "PARAMETRO = 'chave api telecontrol'";
		pstmt = jdbc.getPreparedStatement(sqlSenha);

		rs = pstmt.executeQuery();
		while (rs.next()) {

			senha = rs.getString("VALOR");

		}

		System.out.println(senha);

		jdbc.closeSession();

		return senha;

	}

	public static String getCodProd(String codprod) throws Exception {

		EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = entityFacade.getJdbcWrapper();
		PreparedStatement pstmt = null;
		String referencia = null;
		ResultSet rs = null;

		jdbc.openSession();

		String sqlSenha = "SELECT CODPROD FROM TGFPRO WHERE REFERENCIA = ?";
		pstmt = jdbc.getPreparedStatement(sqlSenha);
		pstmt.setString(1, codprod);

		rs = pstmt.executeQuery();

		while (rs.next()) {

			referencia = rs.getString("CODPROD");

		}

		System.out.println(referencia);

		jdbc.closeSession();

		return referencia;

	}

}
