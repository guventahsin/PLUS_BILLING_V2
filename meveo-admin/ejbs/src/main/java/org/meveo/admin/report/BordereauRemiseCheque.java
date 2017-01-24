/*
 * (C) Copyright 2015-2016 Opencell SAS (http://opencellsoft.com/) and contributors.
 * (C) Copyright 2009-2014 Manaty SARL (http://manaty.net/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * This program is not suitable for any direct or indirect application in MILITARY industry
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.meveo.admin.report;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletResponse;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRCsvDataSource;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.util.JRLoader;

import org.jboss.seam.international.status.Messages;
import org.jboss.seam.international.status.builder.BundleKey;
import org.meveo.admin.exception.BusinessEntityException;
import org.meveo.commons.utils.ParamBean;
import org.meveo.model.crm.Provider;
import org.meveo.model.payments.AccountOperation;
import org.meveo.model.payments.CustomerAccount;
import org.meveo.security.CurrentUser;
import org.meveo.security.MeveoUser;
import org.meveo.service.payments.impl.AccountOperationService;
import org.slf4j.Logger;

@Named
public class BordereauRemiseCheque {

	private static String REPORT_NAME = "REMISE-CHEQUE";
	
	@Inject
	protected Logger log;

    @Inject
    @CurrentUser
    protected MeveoUser currentUser;
    
    @Inject
    private Messages messages;

    private ParamBean paramBean=ParamBean.getInstance();
	
	@Inject
	private AccountOperationService accountOperationService;
	
	public JasperReport jasperReport;

	public JasperPrint jasperPrint;

	public JasperDesign jasperDesign;

	public Map<String, Object> parameters = new HashMap<String, Object>();

	private Date date = new Date();

	public void generateReport() throws BusinessEntityException {
		String fileName = "reports/bordereauRemiseCheque.jasper";
		InputStream reportTemplate = this.getClass().getClassLoader().getResourceAsStream(fileName);
		parameters.put("date", new Date());
		String providerCode = currentUser.getProviderCode();

		String[] occCodes = paramBean.getProperty("report.occ.templatePaymentCheckCodes","RG_CHQ,RG_CHQNI").split(",");
		try {
			jasperReport = (JasperReport) JRLoader.loadObject(reportTemplate);
			File dataSourceFile = generateDataFile(occCodes);
			if (dataSourceFile != null) {
				FacesContext context = FacesContext.getCurrentInstance();
				HttpServletResponse response = (HttpServletResponse) context.getExternalContext()
						.getResponse();
				response.setContentType("application/pdf"); // fill in
				response.setHeader("Content-disposition", "attachment; filename="
						+ generateFileName(providerCode));

				JRCsvDataSource dataSource = createDataSource(dataSourceFile);
				jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
				JasperExportManager.exportReportToPdfFile(jasperPrint,
						generateFileName(providerCode));
				messages.info(new BundleKey("messages", "report.reportCreted"));
				OutputStream os;
				try {
					os = response.getOutputStream();
					JasperExportManager.exportReportToPdfStream(jasperPrint, os);
					os.flush();
					os.close();
					context.responseComplete();
				} catch (IOException e) {
					log.error("failed to export report too PdfStream",e);
				}

			} else {
			    messages.info(new BundleKey("messages", "bordereauRemiseCheque.noData"));
			}
		} catch (JRException e) {
			log.error("JR exception ",e);
		} catch (FileNotFoundException e) {
			log.error("file not found exception ",e);
		}
	}

	public JRCsvDataSource createDataSource(File dataSourceFile) throws FileNotFoundException {
		JRCsvDataSource ds = new JRCsvDataSource(dataSourceFile);
		// DecimalFormat df = new DecimalFormat("0.00");
		NumberFormat nf = NumberFormat.getInstance(Locale.US);
		ds.setNumberFormat(nf);
		ds.setFieldDelimiter(';');
		ds.setRecordDelimiter("\n");
		ds.setUseFirstRowAsHeader(true);
		String[] columnNames = new String[] { "customerAccountId", "title", "name", "firstname",
				"amount" };
		ds.setColumnNames(columnNames);
		return ds;
	}

	public File generateDataFile(String[] occCodes) throws BusinessEntityException {

		List<AccountOperation> records = new ArrayList<AccountOperation>();
		for (String occCode : occCodes) {
            records.addAll(accountOperationService.getAccountOperations(this.date, occCode, currentUser.getProvider()));
		}
		Iterator<AccountOperation> itr = records.iterator();
		try {
			File temp = File.createTempFile("bordereauRemiseChequeDS", ".csv");
			FileWriter writer = new FileWriter(temp);
			writer.append("customerAccountId;title;name;firstname;amount");
			writer.append('\n');
			if (records.size() == 0) {
			    writer.close();
				return null;
			}

			while (itr.hasNext()) {
				AccountOperation ooc = itr.next();
				CustomerAccount ca = ooc.getCustomerAccount();
				writer.append(ca.getCode() + ";");
				writer.append(ca.getName().getTitle().getCode() + ";");
				writer.append(ca.getName() + ";");
				writer.append(ca.getName().getFirstName() + ";");
				writer.append(ooc.getAmount().toString());
				writer.append('\n');
			}
			writer.flush();
			writer.close();
			return temp;
		} catch (IOException e) {
			log.error("failed to generate data file",e);
		}
		return null;
	}

	public String generateFileName(String providerCode) {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		StringBuilder sb = new StringBuilder(providerCode + "_");
		sb.append(REPORT_NAME);
		sb.append("_");
		sb.append(df.format(this.date));
		sb.append(".pdf");
		
		String reportsUrl = paramBean.getProperty("reportsURL","/opt/jboss/files/reports/");
		return reportsUrl + sb.toString();
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public List<BordereauRemiseChequeRecord> convertList(List<Object> rows) {
		List<BordereauRemiseChequeRecord> bordereauRemiseChequeRecords = new ArrayList<BordereauRemiseChequeRecord>();
		return bordereauRemiseChequeRecords;
	}
}
