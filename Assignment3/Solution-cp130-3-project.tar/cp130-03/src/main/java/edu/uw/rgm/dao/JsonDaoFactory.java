package edu.uw.rgm.dao;

import edu.uw.ext.framework.account.AccountException;
import edu.uw.ext.framework.dao.AccountDao;
import edu.uw.ext.framework.dao.DaoFactory;
import edu.uw.ext.framework.dao.DaoFactoryException;


/**
 * Implementation of DaoFactory that creates a FileAccountDao instance.
 *
 * @author Russ Moul
 */
public final class JsonDaoFactory implements DaoFactory {
    /**
     * Instantiates an instance of FileAccountDao.
     *
     * @return a new instance of FileAccountDao
     *
     * @throws DaoFactoryException if instantiation fails
     */
    @Override
    public AccountDao getAccountDao() throws DaoFactoryException {
        try {
            return new JsonAccountDao();
        } catch (final AccountException ex) {
            throw new DaoFactoryException(
                  "Instantiation of JsonDaoFactory failed.", ex);
        }
    }
}

