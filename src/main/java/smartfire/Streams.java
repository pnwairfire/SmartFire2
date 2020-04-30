/*SMARTFIRE: Satellite Mapping Automated Reanalysis Tool for Fire Incident REconciliation
Copyright (C) 2006-Present  USDA Forest Service AirFire Research Team and Sonoma Technology, Inc.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package smartfire;

import com.sti.justice.util.Numbers;
import java.util.List;
import smartfire.database.DatabaseConnection;
import smartfire.database.ReconciliationStream;

public class Streams extends ModelView {
    private final DatabaseConnection conn;
    
    public Streams(Application app) {
        super(app);
        this.conn = app.getAppSettings().getDatabaseConnection();
    }    
    
    /*
     *  Views
     */
    public Object getDynamic(String urlPiece) {
        final ReconciliationStream stream;
        if(Numbers.isWholeNumber(urlPiece)) {
            int id = Integer.parseInt(urlPiece);
            stream = conn.getReconciliationStream().getById(id);
        } else {
            stream = conn.getReconciliationStream().getByNameSlug(urlPiece);
        }         
        if(stream == null) {
            return null;
        }
        return new StreamView(getApp(), stream);
    }

    /*
     *  Support Methods
     */
    public List<ReconciliationStream> getStreams() {
        return conn.getReconciliationStream().getAll();
    }
}
