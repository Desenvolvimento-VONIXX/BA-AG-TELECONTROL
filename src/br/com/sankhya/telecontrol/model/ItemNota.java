package br.com.sankhya.telecontrol.model;

import java.math.BigDecimal;

public class ItemNota {

	private BigDecimal sequencia;
	private BigDecimal codProd;
	private BigDecimal codLoc;
	private BigDecimal codLocDestino;
	private BigDecimal qtdNeg;
	private BigDecimal qtdEntregue;
	private BigDecimal qtdConferida;
	private BigDecimal codEmp;
	private BigDecimal nuNota;
	private BigDecimal vlrUnitLiq;
	private BigDecimal vlrTotalLiq;
	private BigDecimal peso;
	private String codVol;
	private String usoprod;
	private BigDecimal pesoBruto;
	private BigDecimal pesoLiquido;
	private BigDecimal codVend;
	
	private BigDecimal seqOrigem;
	
	public String getCodVol() {
		return codVol;
	}

	public void setCodVol(String codVol) {
		this.codVol = codVol;
	}

	public String getUsoprod() {
		return usoprod;
	}

	public void setUsoprod(String usoprod) {
		this.usoprod = usoprod;
	}

	public BigDecimal getPesoBruto() {
		return pesoBruto;
	}

	public void setPesoBruto(BigDecimal pesoBruto) {
		this.pesoBruto = pesoBruto;
	}

	public BigDecimal getPesoLiquido() {
		return pesoLiquido;
	}

	public void setPesoLiquido(BigDecimal pesoLiquido) {
		this.pesoLiquido = pesoLiquido;
	}

	public BigDecimal getPeso() {
		return peso;
	}

	public void setPeso(BigDecimal peso) {
		this.peso = peso;
	}

	public BigDecimal getQtdEntregue() {
		return qtdEntregue;
	}

	public void setQtdEntregue(BigDecimal qtdEntregue) {
		this.qtdEntregue = qtdEntregue;
	}

	public BigDecimal getQtdConferida() {
		return qtdConferida;
	}

	public void setQtdConferida(BigDecimal qtdConferida) {
		this.qtdConferida = qtdConferida;
	}

	public BigDecimal getCodEmp() {
		return codEmp;
	}

	public void setCodEmp(BigDecimal codEmp) {
		this.codEmp = codEmp;
	}

	public BigDecimal getNuNota() {
		return nuNota;
	}

	public void setNuNota(BigDecimal nuNota) {
		this.nuNota = nuNota;
	}

	public BigDecimal getVlrUnitLiq() {
		return vlrUnitLiq;
	}

	public void setVlrUnitLiq(BigDecimal vlrUnitLiq) {
		this.vlrUnitLiq = vlrUnitLiq;
	}

	public BigDecimal getVlrTotalLiq() {
		return vlrTotalLiq;
	}

	public void setVlrTotalLiq(BigDecimal vlrTotalLiq) {
		this.vlrTotalLiq = vlrTotalLiq;
	}

	public BigDecimal getCodProd() {
		return codProd;
	}

	public void setCodProd(BigDecimal codProd) {
		this.codProd = codProd;
	}

	public BigDecimal getCodLoc() {
		return codLoc;
	}

	public void setCodLoc(BigDecimal codLoc) {
		this.codLoc = codLoc;
	}

	public BigDecimal getQtdNeg() {
		return qtdNeg;
	}

	public void setQtdNeg(BigDecimal qtdNeg) {
		this.qtdNeg = qtdNeg;
	}

	public BigDecimal getSequencia() {
		return sequencia;
	}

	public void setSequencia(BigDecimal sequencia) {
		this.sequencia = sequencia;
	}

	public BigDecimal getSeqOrigem() {
		return seqOrigem;
	}

	public void setSeqOrigem(BigDecimal seqOrigem) {
		this.seqOrigem = seqOrigem;
	}

	public BigDecimal getCodVend() {
		return codVend;
	}

	public void setCodVend(BigDecimal codVend) {
		this.codVend = codVend;
	}

	public BigDecimal getCodLocDestino() {
		return codLocDestino;
	}

	public void setCodLocDestino(BigDecimal codLocDestino) {
		this.codLocDestino = codLocDestino;
	}

}
