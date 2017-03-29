package org.meveo.api.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.meveo.api.dto.ActionStatus;
import org.meveo.api.dto.LanguageIsoDto;
import org.meveo.api.dto.response.GetTradingLanguageResponse;

/**
 * * Web service for managing {@link org.meveo.model.billing.Language} and {@link org.meveo.model.billing.TradingLanguage}.
 * 
 * @author Edward P. Legaspi
 **/
@Path("/language")
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })

public interface TradingLanguageRs extends IBaseRs {

    /**
     * Creates tradingLanguage base on language code. If the language code does not exists, a language record is created.
     * 
     * @param postData
     * @return
     */
    @POST
    @Path("/")
    public ActionStatus create(LanguageIsoDto postData);

    /**
     * Search language given a code.
     * 
     * @param languageCode
     * @return
     */
    @GET
    @Path("/")
    public GetTradingLanguageResponse find(@QueryParam("languageCode") String languageCode);

    /**
     * Does not delete a language but the tradingLanguage associated to it.
     * 
     * @param languageCode
     * @return
     */
    @DELETE
    @Path("/{languageCode}")
    public ActionStatus remove(@PathParam("languageCode") String languageCode);

    /**
     * modify a language. Same input parameter as create. The language and trading Language are created if they don't exists. The operation fails if the tradingLanguage is null.
     * 
     * @param postData
     * @return
     */
    @PUT
    @Path("/")
    public ActionStatus update(LanguageIsoDto postData);

    /**
     * Create or update a language if it doesn't exists
     * 
     * @param postData
     * @return
     */
    @POST
    @Path("/createOrUpdate")
    public ActionStatus createOrUpdate(LanguageIsoDto postData);
}