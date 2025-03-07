package br.com.sankhya.telecontrol.util;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.QueryExecutor;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.metadata.EntityMetaData;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class EnviromentUtils {

	private static ContextoAcao contexto;
	private static String usuarioConectado = "";
	private static Map<String, String> mapPorts = new HashMap<String, String>() {
		{
			put("HOMOLOGA", "8080");
			put("SANKHYA", "8180");
			put("TESTE", "8380");
		}
	};

	public static String getPort(ContextoAcao ctx) throws Exception {
		contexto = ctx;
		return mapPorts.get(getUserDB());
	}

	private static Map<String, String> mapSenhas = new HashMap<String, String>() {
		{
			put("TESTE", "vonixx");
		}
	};

	public static String getSenhas(ContextoAcao ctx) throws Exception {
		contexto = ctx;
		return mapSenhas.get(getUserDB());
	}

	public static Object[] getDadosUser(String usuLogado) throws Exception {
		return findDadosUserDBJape(usuLogado);
	}

	private static Object[] findDadosUserDB(String usuLogado) throws Exception {
		QueryExecutor query = contexto.getQuery();
		query.setParam("CODUSU", usuLogado);
		query.nativeSelect("SELECT NOMEUSU, INTERNO FROM TSIUSU WHERE CODUSU = {CODUSU}");

		Object[] retorno = new Object[2];

		if (query.next()) {
			retorno[0] = query.getString("NOMEUSU");
			retorno[1] = query.getString("INTERNO");
		}

		query.close();

		return retorno;
	}

	private static Object[] findDadosUserDBJape(String usuLogado) throws Exception {
		Object[] retorno = new Object[2];

		JapeWrapper usuarioDAO = JapeFactory.dao(DynamicEntityNames.USUARIO);
		DynamicVO usuarioVO = usuarioDAO.findByPK(new BigDecimal(usuLogado));

		retorno[0] = usuarioVO.getProperty("NOMEUSU").toString();
		retorno[1] = usuarioVO.getProperty("INTERNO").toString();

		return retorno;

	}

	private static String getUserDB() throws SQLException, Exception {
		EntityFacade ef = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = null;

		try {
			jdbc = ef.getJdbcWrapper();
			jdbc.openSession();

			// forma A (Oracle e SQL Server)
			if (jdbc.getDialect() == EntityMetaData.ORACLE_DIALECT) {
				
				//System.out.println(jdbc.getUserName());
				//return findUserDBOracle();
				return jdbc.getUserName();
			} else if (jdbc.getDialect() == EntityMetaData.MSSQL_DIALECT) {
				return findUserDBSQLServer();
			} else {
				throw new RuntimeException("Banco de dados desconhecido");
			}
		} finally {
			JdbcWrapper.closeSession(jdbc);
		}
	}

	private static String findUserDBSQLServer() throws Exception {
		QueryExecutor query = contexto.getQuery();
		query.nativeSelect("SELECT DB_NAME() AS 'USER'");

		String user = null;

		if (query.next()) {
			usuarioConectado = query.getString("USER");
			System.out.println(usuarioConectado);
			return query.getString("USER");
		}

		query.close();

		return user;
	}

	private static String findUserDBOracle() throws Exception {
		QueryExecutor query = contexto.getQuery();
		query.nativeSelect("SELECT USER FROM DUAL");

		String user = null;

		if (query.next()) {
			usuarioConectado = query.getString("USER");
			System.out.println(usuarioConectado);
			return query.getString("USER");
		}

		query.close();

		return user;
	}

	public static Connection conectarSankhya() throws Exception {
		Connection conn = null;
		usuarioConectado = getUserDB();
	//	findUserDBOracle();
		if ("TREINA".equals(usuarioConectado)) {
			conn = conectarSankhyaTreina();
		} else if ("TESTE".equals(usuarioConectado)) {
			conn = conectarSankhyaTeste();
		} else {
			conn = conectarSankhyaProducao();
		}
		return conn;
	}

	public static Connection conectarSankhyaProducao() throws Exception {
		Connection retorno = null;
		try {
			Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
			retorno = DriverManager.getConnection(
					"jdbc:jtds:sqlserver://192.168.0.177:1433", "sankhya",
					"tecsis");
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception(e);
		}
		return retorno;
	}

	public static Connection conectarSankhyaTreina() throws Exception {
		Connection retorno = null;
		try {
			Class.forName("oracle.jdbc.driver.OracleDriver");
			retorno = DriverManager.getConnection(
					"jdbc:oracle:thin:@128.1.0.40:1521:ORCL", "treina",
					"tecsis");
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception(e);
		}
		return retorno;
	}

	public static Connection conectarSankhyaTeste() throws Exception {
		Connection retorno = null;
		try {
			Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
			 retorno = DriverManager.getConnection("jdbc:jtds:sqlserver://192.168.0.177:1433","teste", "tecsis");
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception(e);
		}
		return retorno;
	}

	public static void updateQueryConnection(String querySql) throws Exception {
		Connection connection = EnviromentUtils.conectarSankhya();
		try {
			PreparedStatement preparedStatement = connection.prepareStatement(querySql);
			preparedStatement.executeUpdate();
			preparedStatement.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception(e.getMessage());
		} finally {
			connection.close();
		}
	}
	
	public static void updateQueryConneTeste(String querySql) throws Exception {
		Connection conn = null;
		usuarioConectado = getUserDB();
	//	findUserDBOracle();
		if ("TREINA".equals(usuarioConectado)) {
			conn = conectarSankhyaTreina();
		} else if ("TESTE".equals(usuarioConectado)) {
			conn = conectarSankhyaTeste();
		} else {
			conn = conectarSankhyaProducao();
		}
		try {
			PreparedStatement preparedStatement = conn.prepareStatement(querySql);
			preparedStatement.executeUpdate();
			preparedStatement.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception(e.getMessage());
		} finally {
			conn.close();
		}
	}

	public static void criarEmailFilaEnvio(int seqFila, String corpoMensagem, Integer numUnico, String email, String usuLogado, String assunto) throws Exception {

		String sqlInsertEmail = " INSERT INTO tmdfmg (codfila, dtentrada, status, codcon, tentenvio, mensagem, tipoenvio, maxtentenvio, assunto, email, codusu, dhulttenta, dbhashcode, mimetype, tipodoc, nuchave) VALUES ("
				+ seqFila
				+ ", sysdate, 'Pendente', 0, 1, to_char('"
				+ corpoMensagem
				+ "'), 'E', 3, '"
				+ assunto
				+ "', '"
				+ email
				+ "', "
				+ usuLogado
				+ ", sysdate, 'ebeb9eea17452b275e38d0f76f0eb5e22d37537f', 'text/html', 'N', "
				+ numUnico + " ) ";
		// System.out.println(sqlInsertEmail);
		EnviromentUtils.updateQueryConnection(sqlInsertEmail);
	}
	
	public static Integer converterBlobParaArquivo(String querySql, String nomeArquivo) throws Exception {
		Connection connection = EnviromentUtils.conectarSankhya();
		Integer tamFileInformation = 0;
		try {
			Statement stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery(querySql);			
			if (rs.next()) {	
				tamFileInformation = rs.getInt("TAM");
				InputStream bodyOut = rs.getBinaryStream("ARQANEXO");
			    int c;
			    try {			
				OutputStream f = new FileOutputStream(nomeArquivo);
			      while ((c=bodyOut.read())>-1) {
			        f.write(c);
			      }
			      f.close();
			    } catch (Exception e) {
			      e.printStackTrace();
			    }			
			}
			
			stmt.close();
			rs.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception(e.getMessage());
		} finally {
			connection.close();
		}
	//	return arquivo;
		return tamFileInformation;
	}
	
	
	public static void readBLOBToFileStream(BigDecimal idAnexos, String vArq) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		FileOutputStream file = null;
		try {
			conn = EnviromentUtils.conectarSankhya();
			String sqlArq = " Select a.ARQANEXODI From AD_EMBIMPDIANEXO a Where a.CODEMBIMPDIANEXO = " + idAnexos +  " ";
			pstmt = conn.prepareStatement(sqlArq);
			System.out.println(sqlArq);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				InputStream data = rs.getBinaryStream("ARQANEXODI");
				try {
					file = new FileOutputStream(vArq);
					int chunk;
					while ((chunk = data.read()) != -1)
						file.write(chunk);	

				} catch (Exception e) {
					String err = e.toString();
					System.out.println(err);
				} finally {
					if (file != null) {
						file.close();
					}
				}
			}			
		} catch (Exception e) {
			e.printStackTrace();			
		} finally {
			try {
				pstmt.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			try {
				rs.close();
			} catch (SQLException e) {	
				e.printStackTrace();
			}
		}		
	}
	
	public static String retornarXmlMdeLog(String chaveNfe, String descEvento) throws Exception {
		Connection connection = EnviromentUtils.conectarSankhya();
		String arqXml = "";
		try {
			String sql = "	Select l.xmlevento as arquivo from tgfmdelog l where l.chaveacesso = '" + chaveNfe + "' and l.descrevento = '" + descEvento + "' ";

			PreparedStatement preparedStatement = connection.prepareStatement(sql);
			InputStream inputStream = null;
			ResultSet resultSet = preparedStatement.executeQuery();

			Clob arq = null;
			while (resultSet.next()) {
				arq = resultSet.getClob("arquivo");
				arqXml = resultSet.getNString("arquivo");		
				return arqXml;
			}
			resultSet.close();
			preparedStatement.close();

			connection.close();
		} catch (Exception e) {
			throw new Exception("Erro retornarXmlMdeLog: " + e.toString());
		} finally {
			connection.close();
		}
		return null;
	}
	
	public static String retornarXmlNfe(String chaveNfe) throws Exception {
		Connection connection = EnviromentUtils.conectarSankhya();
		String arqXml = "";
		try {
			String sql = " SELECT nfe.XMLENVCLI as arquivo FROM TGFNFE nfe Where nfe.CHAVENFE = '" + chaveNfe + "' ";

			PreparedStatement preparedStatement = connection.prepareStatement(sql);
			InputStream inputStream = null;
			ResultSet resultSet = preparedStatement.executeQuery();

			Clob arq = null;
			while (resultSet.next()) {
				arq = resultSet.getClob("arquivo");
				arqXml = resultSet.getNString("arquivo");		
				return arqXml;
			}
			resultSet.close();
			preparedStatement.close();

			connection.close();
		} catch (Exception e) {
			throw new Exception("Erro retornarXmlNfe: " + e.toString());
		} finally {
			connection.close();
		}
		return null;
	}
	
	}
